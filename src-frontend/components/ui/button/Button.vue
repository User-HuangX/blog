<script setup>
import { computed, useAttrs } from "vue";
import { cva } from "class-variance-authority";
import { cn } from "@/lib/utils";

defineOptions({ inheritAttrs: false });

const buttonVariants = cva(
    "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-none text-sm font-medium transition-all focus-visible:outline-none disabled:pointer-events-none disabled:opacity-50",
    {
        variants: {
            variant: {
                default: "bg-blue-600 text-white hover:bg-blue-700",
                destructive: "bg-red-600 text-white hover:bg-red-700",
                outline: "border border-slate-200 bg-white text-slate-900 hover:bg-slate-50",
                secondary: "bg-slate-600 text-white hover:bg-slate-700",
                ghost: "text-slate-700 hover:bg-black/5",
                link: "text-blue-700 underline-offset-4 hover:underline",
            },
            size: {
                default: "h-9 px-4 py-2",
                sm: "h-8 px-3 text-xs",
                lg: "h-10 px-8",
                icon: "h-9 w-9",
            },
        },
        defaultVariants: {
            variant: "default",
            size: "default",
        },
    },
);

const props = defineProps({
    variant: { type: String, default: "default" },
    size: { type: String, default: "default" },
    type: { type: String, default: "button" },
    class: { type: null, default: undefined },
});

const attrs = useAttrs();

const delegatedAttrs = computed(() => {
    const { class: _c, ...rest } = attrs;
    return rest;
});

const classes = computed(() =>
    cn(buttonVariants({ variant: props.variant, size: props.size }), attrs.class, props.class),
);
</script>

<template>
    <button :type="type" v-bind="delegatedAttrs" :class="classes">
        <slot />
    </button>
</template>
