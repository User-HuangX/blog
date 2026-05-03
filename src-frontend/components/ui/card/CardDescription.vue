<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
    class: { type: null, default: undefined },
});
</script>

<template>
    <p :class="cn('text-sm text-slate-500', props.class)">
        <slot />
    </p>
</template>
