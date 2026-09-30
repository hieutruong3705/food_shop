import { describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import FilterRange from '../src/components/FilterRange.vue';

const props = { label: 'Giá', min: 490000, max: 16290000, low: 490000, high: 16290000, step: 10000 };
const boxes = (wrapper) => wrapper.findAll('input[type="number"]');

describe('FilterRange', () => {
  it('shows the applied range in the number boxes', () => {
    const wrapper = mount(FilterRange, { props: { ...props, low: 1000000, high: 5000000 } });
    expect(boxes(wrapper).map((b) => b.element.value)).toEqual(['1000000', '5000000']);
  });

  it('reports a typed lower bound and keeps it inside the catalog range', async () => {
    const wrapper = mount(FilterRange, { props });
    await boxes(wrapper)[0].setValue('100');
    await boxes(wrapper)[0].trigger('change');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ low: 490000, high: 16290000 });
    await boxes(wrapper)[0].setValue('2000000');
    await boxes(wrapper)[0].trigger('change');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ low: 2000000, high: 16290000 });
  });

  it('never lets the lower bound pass the upper one', async () => {
    const wrapper = mount(FilterRange, { props: { ...props, low: 1000000, high: 3000000 } });
    await boxes(wrapper)[0].setValue('9000000');
    await boxes(wrapper)[0].trigger('change');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ low: 3000000, high: 3000000 });
  });

  it('follows the slider and reports each move', async () => {
    const wrapper = mount(FilterRange, { props });
    const [from] = wrapper.findAll('input[type="range"]');
    await from.setValue('2000000');
    expect(wrapper.emitted('change').at(-1)[0].low).toBe(2000000);
  });

  it('uses a step of one when the step would hide the top of the range', () => {
    const wrapper = mount(FilterRange, { props: { ...props, min: 490000, max: 16295000 } });
    expect(wrapper.find('input[type="range"]').attributes('step')).toBe('1');
    expect(mount(FilterRange, { props }).find('input[type="range"]').attributes('step')).toBe('10000');
  });

  it('shows only text when every product has the same value', () => {
    const wrapper = mount(FilterRange, { props: { ...props, min: 500000, max: 500000, low: 500000, high: 500000 } });
    expect(wrapper.find('input[type="range"]').exists()).toBe(false);
    expect(wrapper.text()).toContain('Tất cả sản phẩm');
  });
});
