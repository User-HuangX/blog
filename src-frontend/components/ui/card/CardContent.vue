<script setup>
import {cn} from "@/lib/utils";

const props = defineProps({
  class: {type: null, default: undefined},
});
</script>

<template>
  <div :class="cn('p-6 pt-4', props.class)">
    <slot/>
  </div>
</template>
