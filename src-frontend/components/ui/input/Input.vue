<script setup>
import { computed, useAttrs } from "vue";
import { cn } from "@/lib/utils";

defineOptions({ inheritAttrs: false });

const model = defineModel({ type: String, default: "" });

const props = defineProps({
    class: { type: null, default: undefined },
});

const attrs = useAttrs();

const delegatedAttrs = computed(() => {
    const { class: _c, ...rest } = attrs;
    return rest;
});
</script>

<template>
    <input
        v-model="model"
        v-bind="delegatedAttrs"
        :class="cn('flex h-9 w-full rounded-none border border-slate-200 bg-white px-3 py-1 text-sm text-slate-900 transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-slate-400 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-500/20 focus-visible:border-rose-300 disabled:cursor-not-allowed disabled:opacity-50', attrs.class, props.class)"
    />
</template>
