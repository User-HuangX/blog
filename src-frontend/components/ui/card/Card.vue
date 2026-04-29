<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
    class: { type: null, default: undefined },
});
</script>

<template>
    <div
        :class="
            cn(
                'rounded-xl border border-slate-200/80 bg-white/95 text-slate-900 shadow-sm backdrop-blur-sm',
                props.class,
            )
        "
    >
        <slot />
    </div>
</template>
