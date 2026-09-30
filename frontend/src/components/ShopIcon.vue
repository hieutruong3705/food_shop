<script setup>
import { MagnifyingGlassIcon, ShoppingBagIcon, UserCircleIcon, Bars3Icon, ArrowRightIcon, TruckIcon, ShieldCheckIcon, CreditCardIcon, BoltIcon, Squares2X2Icon, ChevronRightIcon } from '@heroicons/vue/24/outline';
defineProps({ name: String });
const icons = { search: MagnifyingGlassIcon, cart: ShoppingBagIcon, user: UserCircleIcon, menu: Bars3Icon, arrow: ArrowRightIcon, truck: TruckIcon, shield: ShieldCheckIcon, payment: CreditCardIcon, bolt: BoltIcon, grid: Squares2X2Icon, chevron: ChevronRightIcon };
</script>
<template><component :is="icons[name] || Squares2X2Icon" class="shop-icon" aria-hidden="true" /></template>
