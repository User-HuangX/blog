<script setup>
import { AgGridVue } from "ag-grid-vue3";
import { computed, shallowRef, watch } from "vue";

const ACTION_CELL = "text-red-600 font-semibold cursor-pointer hover:text-red-700";

const props = defineProps({
    posts: { type: Array, default: () => [] },
    deleteMode: { type: Boolean, default: false },
    editMode: { type: Boolean, default: false },
    isAuthor: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
});

const emit = defineEmits(["open", "delete", "edit"]);

const gridApi = shallowRef(null);

function onGridReady(params) {
    gridApi.value = params.api;
}

watch(
    () => [props.deleteMode, props.editMode, props.isAuthor],
    () => {
        gridApi.value?.refreshCells({ force: true });
        gridApi.value?.redrawRows();
    },
);

const columnDefs = computed(() => {
    const cols = [
        {
            field: "title",
            headerName: "标题",
            flex: 2,
            minWidth: 200,
            filter: "agTextColumnFilter",
            cellClass: "post-list-title-cell text-slate-900 font-medium text-[1rem] leading-tight",
        },
    ];
    if (props.editMode && props.isAuthor) {
        cols.push({
            colId: "edit-actions",
            headerName: "操作",
            width: 140,
            maxWidth: 180,
            sortable: false,
            filter: false,
            resizable: false,
            valueGetter: () => "修改",
            cellClass: ACTION_CELL,
        });
    } else if (props.deleteMode && props.isAuthor) {
        cols.push({
            colId: "delete-actions",
            headerName: "操作",
            width: 140,
            maxWidth: 180,
            sortable: false,
            filter: false,
            resizable: false,
            valueGetter: () => "删除",
            cellClass: ACTION_CELL,
        });
    }
    return cols;
});

const defaultColDef = {
    sortable: true,
    resizable: true,
    filter: true,
    flex: 1,
    minWidth: 100,
};

function onCellClicked(event) {
    const id = event.data?.id;
    if (id == null) return;
    const colId = event.column?.getColId?.() ?? event.column?.colId;
    if (colId === "title") {
        emit("open", id);
    }
    if (colId === "delete-actions" && props.deleteMode && props.isAuthor) {
        emit("delete", id);
    }
    if (colId === "edit-actions" && props.editMode && props.isAuthor) {
        emit("edit", id);
    }
}
</script>

<template>
    <div class="w-full">
        <p v-if="loading" class="mb-3 text-sm text-slate-500">加载中...</p>
        <div
            v-show="!loading"
            class="post-list-grid ag-theme-quartz w-full rounded-2xl border border-slate-200/70 bg-slate-100/40 shadow-inner"
            style="height: min(60vh, 560px)"
        >
            <AgGridVue
                class="h-full w-full min-w-0"
                :row-data="posts"
                :column-defs="columnDefs"
                :default-col-def="defaultColDef"
                :row-height="72"
                :header-height="48"
                :animate-rows="true"
                :suppress-cell-focus="true"
                @grid-ready="onGridReady"
                @cell-clicked="onCellClicked"
            />
        </div>
    </div>
</template>

<style scoped>
.post-list-grid :deep(.ag-header-viewport),
.post-list-grid :deep(.ag-body-viewport) {
    padding-left: 14px;
    padding-right: 14px;
}

.post-list-grid :deep(.ag-header-cell) {
    padding-left: 18px;
    padding-right: 18px;
    font-size: 0.8125rem;
    font-weight: 600;
    letter-spacing: 0.02em;
    color: rgb(71 85 105);
}

.post-list-grid :deep(.ag-row) {
    cursor: pointer;
    border-radius: 14px;
    border: 1px solid rgb(226 232 240);
    background-color: rgb(255 255 255);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
    overflow: hidden;
    transition:
        box-shadow 0.2s ease,
        border-color 0.2s ease,
        background-color 0.2s ease;
}

.post-list-grid :deep(.ag-cell) {
    padding-left: 20px;
    padding-right: 20px;
    display: flex;
    align-items: center;
}

/* 标题列：减小相对行高的上下留白（行高由 row-height 控制，此处收紧文字行距与垂直对齐） */
.post-list-grid :deep(.post-list-title-cell) {
    line-height: 1.3;
    padding-top: 0;
    padding-bottom: 0;
    align-items: center;
}

/* 行内单元格背景与行一致，悬浮时整行统一变色 */
.post-list-grid :deep(.ag-row .ag-cell) {
    background-color: transparent !important;
}

.post-list-grid :deep(.ag-row:hover),
.post-list-grid :deep(.ag-row.ag-row-hover) {
    border-color: rgb(191 219 254);
    box-shadow: 0 10px 28px rgba(15, 23, 42, 0.1);
}

.post-list-grid :deep(.ag-row:hover .ag-cell),
.post-list-grid :deep(.ag-row.ag-row-hover .ag-cell) {
    background-color: rgb(239 246 255) !important;
}
</style>
