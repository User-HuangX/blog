<script setup>
import { AgGridVue } from "ag-grid-vue3";
import { computed, shallowRef, watch } from "vue";

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
            cellClass: "post-list-title-cell",
        },
    ];
    if (props.editMode && props.isAuthor) {
        cols.push({
            colId: "edit-actions",
            headerName: "",
            width: 80,
            maxWidth: 100,
            sortable: false,
            filter: false,
            resizable: false,
            valueGetter: () => "修改",
            cellClass: "action-cell",
        });
    } else if (props.deleteMode && props.isAuthor) {
        cols.push({
            colId: "delete-actions",
            headerName: "",
            width: 80,
            maxWidth: 100,
            sortable: false,
            filter: false,
            resizable: false,
            valueGetter: () => "删除",
            cellClass: "action-cell-danger",
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
        <p v-if="loading" class="mb-3 text-sm" style="color:#8a817a">加载中...</p>
        <div
            v-show="!loading"
            class="post-list-grid ag-theme-quartz w-full"
            style="height: min(60vh, 560px)"
        >
            <AgGridVue
                class="h-full w-full min-w-0"
                :row-data="posts"
                :column-defs="columnDefs"
                :default-col-def="defaultColDef"
                :row-height="64"
                :header-height="42"
                :animate-rows="true"
                :suppress-cell-focus="true"
                @grid-ready="onGridReady"
                @cell-clicked="onCellClicked"
            />
        </div>
    </div>
</template>

<style scoped>
.post-list-grid :deep(.ag-root-wrapper) {
    border: 1px solid #e5ddd5;
    border-radius: 0;
    background: rgba(255, 255, 255, 0.45);
}

.post-list-grid :deep(.ag-header) {
    background: transparent;
    border-bottom: 1px solid #ede7e0;
}

.post-list-grid :deep(.ag-header-cell) {
    font-size: 11px;
    font-weight: 600;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: #8a817a;
}

.post-list-grid :deep(.ag-row) {
    border-bottom: 1px solid #ede7e0;
    transition: background-color 0.15s ease;
}

.post-list-grid :deep(.ag-row):last-child {
    border-bottom: none;
}

.post-list-grid :deep(.ag-cell) {
    display: flex;
    align-items: center;
    padding-left: 20px;
    padding-right: 20px;
}

.post-list-grid :deep(.post-list-title-cell) {
    font-size: 15px;
    font-weight: 400;
    color: #1e1b18;
    line-height: 1.4;
    cursor: pointer;
    transition: color 0.15s ease;
}

.post-list-grid :deep(.post-list-title-cell):hover {
    color: #c2594a;
}

.post-list-grid :deep(.action-cell),
.post-list-grid :deep(.action-cell-danger) {
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    text-align: center;
}

.post-list-grid :deep(.action-cell) {
    color: #c2594a;
}

.post-list-grid :deep(.action-cell-danger) {
    color: #b43c3c;
}

.post-list-grid :deep(.ag-row:hover) {
    background-color: rgba(194, 89, 74, 0.04);
}

.post-list-grid :deep(.ag-row:hover .ag-cell) {
    background-color: transparent !important;
}
</style>
