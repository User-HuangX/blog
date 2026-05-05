<script setup>
import {cn} from "@/lib/utils";

const props = defineProps({
  class: {type: null, default: undefined},
});
</script>

<template>
  <div :class="cn('rounded-none border border-slate-200 bg-white text-slate-900 shadow-sm', props.class)">
    <slot/>
  </div>
</template>
