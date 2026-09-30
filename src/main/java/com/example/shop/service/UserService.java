package com.example.shop.service;

import com.example.shop.domain.Order;
import com.example.shop.domain.Role;
import com.example.shop.domain.User;
import com.example.shop.domain.dto.RegisterDTO;
import com.example.shop.repository.OrderRepository;
import com.example.shop.repository.RoleRepository;
import com.example.shop.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final long VERIFICATION_VALID_SECONDS = 86400;
    private static final long RESEND_INTERVAL_SECONDS = 60;

    /** Everything needed to e-mail a verification link. The code itself is never logged. */
    public record VerificationIssue(long userId, String email, String fullName, String code) {}

    @org.springframework.beans.factory.annotation.Value("${shop.mail-from:no-reply@techshop.example}")
    private String mailFrom;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender; // 1. Thêm JavaMailSender
    private final OrderRepository orderRepository;
    private final Clock clock;

    // Cập nhật Constructor để Inject JavaMailSender
    public UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, JavaMailSender mailSender, OrderRepository orderRepository, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.orderRepository = orderRepository;
        this.clock = clock;
    }

    // Lưu thông tin người dùng vào cơ sở dữ liệu
    public User handleSaveUser(User user) {
        return userRepository.save(user);
    }

    // Đăng ký người dùng mới (CẬP NHẬT: Thêm logic tạo mã xác thực và set enabled =
    // false)
    public User registerUser(User user) {
        Role role = roleRepository.findByName("USER");
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // --- THÊM MỚI ---
        user.setVerificationCode(UUID.randomUUID().toString()); // Tạo mã xác thực
        user.setVerificationExpiresAt(clock.instant().plusSeconds(86400));
        user.setEnabled(false); // Mặc định chưa kích hoạt
        // ----------------

        return this.handleSaveUser(user);
    }

    // --- CÁC HÀM MỚI CHO CHỨC NĂNG XÁC THỰC EMAIL ---

    /**
     * Registers a new account, or restarts an unverified one whose link has expired (the old
     * name, password and code are replaced). The account keeps the one-minute resend slot claimed
     * until {@link #releaseVerificationSlot} is called after a failed send.
     */
    @Transactional
    public VerificationIssue register(String firstName, String lastName, String email, String rawPassword) {
        Instant now = clock.instant();
        User user = userRepository.existsByEmail(email) ? userRepository.lockByEmail(email) : null;
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setRole(roleRepository.findByName("USER"));
        } else if (!isExpiredPendingRegistration(user, now)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }
        user.setFullName(firstName + " " + lastName);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(false);
        return issue(user, now);
    }

    /**
     * Issues a fresh code for a registration that is still waiting for verification, at most once
     * a minute. Returns null when nothing should be sent, so callers cannot tell the reasons apart.
     */
    @Transactional
    public VerificationIssue issueVerificationResend(String email) {
        if (email == null || !userRepository.existsByEmail(email)) return null;
        Instant now = clock.instant();
        User user = userRepository.lockByEmail(email);
        if (user == null || !isPendingRegistration(user)) return null;
        Instant last = user.getVerificationSentAt();
        if (last != null && last.plusSeconds(RESEND_INTERVAL_SECONDS).isAfter(now)) return null;
        return issue(user, now);
    }

    private VerificationIssue issue(User user, Instant now) {
        String code = UUID.randomUUID().toString();
        user.setVerificationCode(code);
        user.setVerificationExpiresAt(now.plusSeconds(VERIFICATION_VALID_SECONDS));
        user.setVerificationSentAt(now);
        user = userRepository.save(user);
        return new VerificationIssue(user.getId(), user.getEmail(), user.getFullName(), code);
    }

    /** An account created by registration that has not been verified. Admin-disabled accounts have no code. */
    private static boolean isPendingRegistration(User user) {
        return !user.isEnabled() && !user.getIsLocked() && user.getVerificationCode() != null;
    }

    private static boolean isExpiredPendingRegistration(User user, Instant now) {
        return isPendingRegistration(user)
                && (user.getVerificationExpiresAt() == null || user.getVerificationExpiresAt().isBefore(now));
    }

    /** Lets the user retry immediately after the e-mail could not be handed to the mail server. */
    public void releaseVerificationSlot(long userId) {
        userRepository.clearVerificationSentAt(userId);
    }

    /** Sends the verification e-mail; a mail failure is logged and reported, never thrown. */
    public boolean sendVerificationQuietly(VerificationIssue issue, String siteURL) {
        try {
            sendVerificationEmail(issue.email(), issue.fullName(), issue.code(), siteURL);
            return true;
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
            log.warn("Could not send the verification e-mail for user {}: {}: {}", issue.userId(),
                    cause.getClass().getSimpleName(), cause.getMessage());
            releaseVerificationSlot(issue.userId());
            return false;
        }
    }

    // Gửi email xác thực đăng ký
    public void sendVerificationEmail(String toAddress, String fullName, String code, String siteURL)
            throws MessagingException, UnsupportedEncodingException {
        String fromAddress = mailFrom;
        String senderName = "TechShop Support";
        String subject = "Vui lòng xác thực đăng ký tài khoản";
        String content = "Chào [[name]],<br>"
                + "Vui lòng nhấn vào liên kết dưới đây để kích hoạt tài khoản của bạn:<br>"
                + "<h3><a href=\"[[URL]]\" target=\"_self\">XÁC THỰC NGAY</a></h3>"
                + "Cảm ơn,<br>"
                + "TechShop Team.";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", org.springframework.web.util.HtmlUtils.htmlEscape(fullName == null ? "" : fullName));
        String verifyURL = siteURL + "/verify?code=" + code;
        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);
        mailSender.send(message);
    }

    // Xử lý khi người dùng bấm link xác thực
    @org.springframework.transaction.annotation.Transactional
    public boolean verify(String verificationCode) {
        User user = userRepository.lockByVerificationCode(verificationCode);

        if (user == null || user.isEnabled() || user.getVerificationExpiresAt()==null || user.getVerificationExpiresAt().isBefore(clock.instant())) {
            return false; // Code sai hoặc đã kích hoạt rồi
        } else {
            user.setVerificationCode(null); // Xóa code đi
            user.setEnabled(true); // Kích hoạt tài khoản
            userRepository.save(user);
            return true;
        }
    }

    // --- CÁC HÀM MỚI CHO CHỨC NĂNG QUÊN MẬT KHẨU ---

    // 1. Tạo token reset password và lưu vào DB
    public void updateResetPasswordToken(String token, String email) throws Exception {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setResetPasswordToken(token);
            user.setResetPasswordExpiresAt(clock.instant().plusSeconds(3600));
            userRepository.save(user);
        } else {
            throw new Exception("Không tìm thấy email: " + email);
        }
    }

    // 2. Tìm user bằng token reset password
    @org.springframework.transaction.annotation.Transactional
    public User getByResetPasswordToken(String token) {
        if(token==null || token.isBlank())return null;
        User user=userRepository.lockByResetToken(token);
        return user!=null && user.getResetPasswordExpiresAt()!=null && user.getResetPasswordExpiresAt().isAfter(clock.instant())?user:null;
    }

    // 3. Cập nhật mật khẩu mới và xóa token
    // 3. Cập nhật mật khẩu mới và xóa token
    public void updatePassword(User user, String newPassword) {
        // QUAN TRỌNG: Ở Controller mình đã mã hóa rồi, nên ở đây KHÔNG mã hóa nữa
        // Chỉ gán trực tiếp vào thôi.
        user.setPassword(newPassword);

        user.setResetPasswordToken(null); // Xóa token reset nếu có
        user.setResetPasswordExpiresAt(null);
        userRepository.save(user);
    }

    // 4. Gửi email chứa link reset password
    public void sendEmail(String recipientEmail, String link) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(mailFrom, "TechShop Support");
        helper.setTo(recipientEmail);

        String subject = "Yêu cầu đặt lại mật khẩu";
        String content = "<p>Xin chào,</p>"
                + "<p>Bạn đã yêu cầu đặt lại mật khẩu.</p>"
                + "<p>Nhấn vào link bên dưới để đổi mật khẩu:</p>"
                + "<p><a href=\"" + link + "\">Đặt lại mật khẩu</a></p>"
                + "<br>"
                + "<p>Bỏ qua email này nếu bạn nhớ mật khẩu của mình.</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    // ---------------------------------------------------------

    // Tìm kiếm người dùng theo địa chỉ email
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Lấy danh sách tất cả người dùng trong hệ thống
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Tìm kiếm người dùng theo ID
    public User getUserById(long id) {
        return this.userRepository.findById(id).orElse(null);
    }

    // Đảo trạng thái khóa/mở khóa tài khoản người dùng
    // Trong UserService.java
    // --- Trong file UserService.java ---

    // 1. Sửa lại hàm toggleLockUser (Nhận thêm currentLoginId)
    public void toggleLockUser(long id, long currentLoginId) {
        // Check chặn tự khóa
        if (id == currentLoginId) {
            throw new RuntimeException("Bạn không thể tự khóa tài khoản của chính mình!");
        }

        User user = this.getUserById(id);
        if (user != null) {
            // Check chặn nếu còn đơn hàng (Nhớ là bạn đã thêm OrderRepository vào
            // UserService rồi nhé)
            // Nếu chưa thêm OrderRepository thì tạm thời bỏ qua đoạn check đơn hàng này
            /*
             * List<Order> runningOrders = orderRepository.findByUserAndStatusNotIn(user,
             * List.of("COMPLETE", "CANCEL"));
             * if (!runningOrders.isEmpty()) {
             * throw new RuntimeException("Tài khoản này còn đơn hàng chưa hoàn thành!");
             * }
             */

            user.setIsLocked(!user.getIsLocked());
            this.userRepository.save(user);
        }
    }

    // 2. Sửa lại hàm deleteUserById (Nhận thêm currentLoginId)
    public void deleteUserById(long id, long currentLoginId) {
        // Check chặn tự xóa
        if (id == currentLoginId) {
            throw new RuntimeException("Bạn không thể tự xóa tài khoản của chính mình!");
        }

        User user = this.getUserById(id);
        if (user != null) {
            if (user.getCart() != null) {
                user.setCart(null);
            }
            userRepository.deleteById(id);
        }
    }

    // Lấy thông tin quyền hạn theo tên role
    public Role getRoleByName(String name) {
        return this.roleRepository.findByName(name);
    }

    // Chuyển đổi dữ liệu từ RegisterDTO sang thực thể User
    public User registerDTOtoUser(RegisterDTO registerDTO) {
        User user = new User();
        user.setFullName(registerDTO.getFirstName() + " " + registerDTO.getLastName());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(registerDTO.getPassword());
        return user;
    }

    // Kiểm tra xem email đã tồn tại trong hệ thống chưa
    public boolean checkEmailExists(String email) {
        return this.userRepository.existsByEmail(email);
    }

    // Cập nhật thông tin cá nhân của người dùng
    public void updateUserProfile(long userId, User updatedUser) {
        User currentUser = this.getUserById(userId);
        if (currentUser != null) {
            currentUser.setFullName(updatedUser.getFullName());
            currentUser.setPhone(updatedUser.getPhone()); // Dòng này cực kỳ quan trọng
            currentUser.setAddress(updatedUser.getAddress());
            this.userRepository.save(currentUser);
        }
    }

    // Đếm tổng số lượng người dùng trong hệ thống
    public long countAllUsers() {
        return userRepository.count();
    }

    // Thêm vào trong class UserService
    public List<User> searchUsers(String keyword, String roleName, Boolean status) {
        return userRepository.searchUsers(keyword, roleName, status);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
}
