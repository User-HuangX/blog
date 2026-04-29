<template>
    <div class="layout">
        <!-- ═══ Topbar ═══ -->
        <header class="topbar">
            <div class="topbar-inner">
                <h1>阅读我</h1>
                <div class="flex flex-wrap items-center gap-2">
                    <button
                        type="button"
                        class="btn"
                        :class="isAuthor ? 'btn--warm' : 'btn--primary'"
                        @click="onAuthorAction"
                    >
                        {{ isAuthor ? "作者" : "登录" }}
                    </button>
                    <button v-if="isAuthor" type="button" class="btn btn--ghost" @click="logoutAuthor">
                        退出
                    </button>
                </div>
            </div>
        </header>

        <!-- ═══ Main ═══ -->
        <main class="container">
            <Transition name="view-switch" mode="out-in">

                <!-- ─── Write / Edit View ─── -->
                <div
                    v-if="currentView === 'write'"
                    key="compose-view"
                    class="editorial-card"
                >
                    <div class="editorial-header">
                        <div>
                            <h2 class="editorial-title">
                                {{ editingRouteId != null ? "编辑文章" : "写点东西" }}
                            </h2>
                            <p class="editorial-subtitle">
                                {{
                                    editingRouteId != null
                                        ? "修改标题与正文，保存后同步更新。"
                                        : "记录你的想法，支持超链接、图片、表格与多种排版。"
                                }}
                            </p>
                        </div>
                        <button type="button" class="btn btn--ghost btn--sm" @click="goBackToList">返回列表</button>
                    </div>

                    <div class="editorial-body">
                        <form class="compose-form" @submit.prevent="onComposeSubmit">
                            <label>
                                标题
                                <input
                                    v-model.trim="draft.title"
                                    maxlength="200"
                                    placeholder="给文章起一个醒目的标题"
                                    required
                                    type="text"
                                />
                            </label>

                            <label>正文</label>
                            <div class="editor-shell">
                                <aside class="editor-toolbar" aria-label="正文格式">
                                    <select v-model="fontSizeRef" @change="changeFontSize">
                                        <option value="1">12px</option>
                                        <option value="2">14px</option>
                                        <option value="3">16px</option>
                                        <option value="4">18px</option>
                                        <option value="5">24px</option>
                                    </select>
                                    <button type="button" @click="setParagraph('P')">正文</button>
                                    <button type="button" @click="setParagraph('H2')">H2</button>
                                    <button type="button" @click="setParagraph('H3')">H3</button>
                                    <button type="button" @click="applyCommand('bold')"><b>加粗</b></button>
                                    <button type="button" @click="applyCommand('italic')"><i>斜体</i></button>
                                    <button type="button" @click="applyCommand('underline')"><u>下划线</u></button>
                                    <button type="button" @click="applyCommand('strikeThrough')"><s>删除线</s></button>
                                    <button type="button" @click="applyCommand('insertUnorderedList')">· 无序列表</button>
                                    <button type="button" @click="applyCommand('insertOrderedList')">1. 有序列表</button>
                                    <button type="button" @click="applyCommand('formatBlock', 'blockquote')">引用</button>
                                    <button type="button" @click="applyCommand('insertHorizontalRule')">分割线</button>
                                    <button type="button" @click="insertLink">超链接</button>
                                    <button type="button" @click="applyCommand('unlink')">取消链接</button>
                                    <button type="button" @click="insertRemoteImage">网络图</button>
                                    <button type="button" @click="selectImage">上传图片</button>
                                    <button type="button" @click="insertTable">插入表格</button>
                                    <input
                                        ref="imageInputRef"
                                        type="file"
                                        accept="image/*"
                                        class="hidden-input"
                                        @change="uploadImage"
                                    />
                                </aside>
                                <div
                                    ref="editorRef"
                                    class="rich-editor"
                                    contenteditable="true"
                                    @input="syncEditorContent"
                                ></div>
                            </div>

                            <div class="compose-actions">
                                <button type="button" class="btn btn--ghost btn--sm" @click="loadLatestDraft(true)">
                                    加载草稿
                                </button>
                                <button type="button" class="btn btn--ghost btn--sm" @click="saveDraft">保存草稿</button>
                                <button type="submit" class="btn btn--primary">
                                    {{ editPublished ? "保存修改" : "发布文章" }}
                                </button>
                                <span class="text-sm" style="color:#8a817a">{{ formMsg }}</span>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- ─── List / Detail View ─── -->
                <div v-else key="reading-view" class="editorial-card">
                    <div class="editorial-header">
                        <div>
                            <h2 class="editorial-title">{{ !currentPostId ? "文章" : "详情" }}</h2>
                            <span v-if="editMode && !currentPostId" class="status-badge status-badge--edit">编辑模式</span>
                            <span v-if="deleteMode && !currentPostId" class="status-badge status-badge--delete">删除模式</span>
                        </div>
                        <div class="flex flex-wrap items-center gap-2">
                            <span class="text-sm" style="color:#8a817a">{{ postCountLabel }}</span>
                            <button v-if="currentPostId" type="button" class="btn btn--ghost btn--sm" @click="goBackToList">返回</button>
                            <button type="button" class="btn btn--ghost btn--sm" @click="loadPosts">刷新</button>
                        </div>
                    </div>

                    <div class="editorial-body">
                        <Transition name="view-switch" mode="out-in">

                            <!-- Post List -->
                            <div v-if="!currentPostId" key="post-list">
                                <p v-if="errorMsg" class="mb-3 text-sm" style="color:#b43c3c">{{ errorMsg }}</p>
                                <p v-else-if="!loading && posts.length === 0" class="mb-3 text-sm" style="color:#8a817a">
                                    还没有文章，先发布第一篇吧。
                                </p>
                                <PostListGrid
                                    v-else
                                    :posts="posts"
                                    :loading="loading"
                                    :delete-mode="deleteMode"
                                    :edit-mode="editMode"
                                    :is-author="isAuthor"
                                    @open="goToDetail"
                                    @delete="deletePost"
                                    @edit="editPost"
                                />
                            </div>

                            <!-- Post Detail -->
                            <div v-else key="post-detail" id="post-detail">
                                <p v-if="detailLoading" class="text-sm" style="color:#8a817a">加载中...</p>
                                <p v-else-if="detailErrorMsg" class="text-sm" style="color:#b43c3c">{{ detailErrorMsg }}</p>
                                <article v-else-if="detailPost" class="post-item">
                                    <h4>{{ detailPost.title }}</h4>
                                    <div class="post-content" v-html="detailPost.content"></div>
                                </article>
                            </div>
                        </Transition>
                    </div>
                </div>
            </Transition>
        </main>

        <!-- ═══ Author Verification Dialog ═══ -->
        <DialogRoot v-model:open="authorModalVisible" @update:open="onAuthorDialogOpen">
            <DialogPortal>
                <DialogOverlay class="dialog-overlay fixed inset-0 z-[100]" />
                <DialogContent
                    class="dialog-content fixed left-1/2 top-1/2 z-[110] w-[min(420px,calc(100vw-32px))] -translate-x-1/2 -translate-y-1/2 max-h-[80vh] overflow-y-auto p-6 outline-none"
                >
                    <DialogTitle class="dialog-title">作者验证</DialogTitle>
                    <DialogDescription class="dialog-description mt-1 text-sm">输入密码以开启作者模式</DialogDescription>
                    <label class="mt-4 flex flex-col gap-2 text-sm font-medium" style="color:#6b6560">
                        作者密码
                        <input
                            v-model="authorCandidate"
                            type="password"
                            class="dialog-input"
                            placeholder="请输入作者密码"
                            autocomplete="current-password"
                            @keydown.enter.prevent="verifyAuthor"
                        />
                    </label>
                    <div class="mt-5 flex flex-wrap gap-2">
                        <button type="button" class="btn btn--primary" @click="verifyAuthor">验证</button>
                        <button type="button" class="btn btn--ghost" @click="closeAuthorModal">取消</button>
                    </div>
                    <p class="mt-3 text-sm" style="color:#8a817a">{{ authorMsg }}</p>
                </DialogContent>
            </DialogPortal>
        </DialogRoot>

        <!-- ═══ Author Action Dialog ═══ -->
        <DialogRoot v-model:open="authorActionModalVisible">
            <DialogPortal>
                <DialogOverlay class="dialog-overlay fixed inset-0 z-[100]" />
                <DialogContent
                    class="dialog-content fixed left-1/2 top-1/2 z-[110] w-[min(420px,calc(100vw-32px))] -translate-x-1/2 -translate-y-1/2 p-6 outline-none"
                >
                    <DialogTitle class="dialog-title">作者操作</DialogTitle>
                    <DialogDescription class="dialog-description mt-1 text-sm">请选择你要执行的操作。</DialogDescription>
                    <div class="mt-5 flex flex-wrap gap-2">
                        <button type="button" class="btn btn--primary" @click="openPublishMode">发布文章</button>
                        <button type="button" class="btn btn--warm" @click="openEditMode">修改文章</button>
                        <button type="button" class="btn btn--danger" @click="openDeleteMode">删除文章</button>
                        <button type="button" class="btn btn--ghost" @click="closeAuthorActionModal">取消</button>
                    </div>
                </DialogContent>
            </DialogPortal>
        </DialogRoot>
    </div>
</template>

<script setup>
import {
    DialogContent,
    DialogDescription,
    DialogOverlay,
    DialogPortal,
    DialogRoot,
    DialogTitle,
} from "radix-vue";
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import PostListGrid from "@/components/PostListGrid.vue";

const API_BASE = import.meta.env.VITE_API_BASE || "/api";

const posts = ref([]);
const loading = ref(false);
const errorMsg = ref("");
const currentPostId = ref(null);
const currentView = ref("list");
const detailPost = ref(null);
const detailLoading = ref(false);
const detailErrorMsg = ref("");
const editorRef = ref(null);
const imageInputRef = ref(null);
const fontSizeRef = ref("3");

const isAuthor = ref(false);
const authorCandidate = ref("");
const authorMsg = ref("");
const authorModalVisible = ref(false);
const authorActionModalVisible = ref(false);
const deleteMode = ref(false);
const editMode = ref(false);
const editingRouteId = ref(null);
const editPublished = ref(false);

const formMsg = ref("");
const draftId = ref(null);
const draftLoading = ref(false);
const autoSaveTimer = ref(null);
const autoSaving = ref(false);
const suppressAutoSave = ref(false);
const draft = reactive({
    title: "",
    content: "",
});

const postCountLabel = computed(() => `${posts.value.length} 篇`);

function apiUrl(path) {
    return `${API_BASE}${path}`;
}

function apiFetch(path, init = {}) {
    return fetch(apiUrl(path), { ...init, credentials: "include" });
}

function onAuthorDialogOpen(open) {
    if (!open) {
        authorMsg.value = "";
    }
}

async function loadPosts() {
    loading.value = true;
    errorMsg.value = "";
    try {
        const response = await apiFetch("/posts");
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        posts.value = await response.json();
    } catch (error) {
        errorMsg.value = `加载失败: ${error.message}`;
    } finally {
        loading.value = false;
    }
}

async function saveDraft() {
    await persistDraft(false);
}

async function persistDraft(silent = false) {
    if (!isAuthor.value) {
        if (!silent) {
            formMsg.value = "请先验证作者身份。";
        }
        return;
    }
    if (!draft.title.trim()) {
        if (!silent) {
            formMsg.value = "请先填写标题再保存草稿。";
        }
        return;
    }
    if (autoSaving.value) return;
    autoSaving.value = true;
    if (!silent) {
        formMsg.value = editPublished.value ? "保存中..." : "草稿保存中...";
    }
    try {
        if (editPublished.value && draftId.value) {
            const response = await apiFetch(`/posts/${draftId.value}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ title: draft.title, content: draft.content }),
            });
            if (!response.ok) {
                const body = await response.json().catch(() => ({}));
                if (response.status === 403) await logoutAuthor();
                throw new Error(body.message ?? `HTTP ${response.status}`);
            }
            formMsg.value = silent ? `已自动保存于 ${new Date().toLocaleTimeString()}` : "已保存修改。";
            if (!silent) await loadPosts();
            return;
        }

        const response = await apiFetch("/posts/drafts", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id: draftId.value, title: draft.title, content: draft.content }),
        });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (response.status === 403) await logoutAuthor();
            throw new Error(body.message ?? `HTTP ${response.status}`);
        }
        const saved = await response.json();
        draftId.value = saved.id ?? draftId.value;
        formMsg.value = silent ? `草稿自动保存于 ${new Date().toLocaleTimeString()}` : "草稿已保存。";
    } catch (error) {
        formMsg.value = silent ? `自动保存失败: ${error.message}` : `保存草稿失败: ${error.message}`;
    } finally {
        autoSaving.value = false;
    }
}

async function onComposeSubmit() {
    if (editPublished.value) {
        await persistDraft(false);
        return;
    }
    await publishPost();
}

async function publishPost() {
    if (!isAuthor.value) {
        formMsg.value = "请先验证作者身份。";
        return;
    }
    if (!draft.title.trim()) {
        formMsg.value = "请先填写标题再发布。";
        return;
    }
    if (!draftId.value) {
        await saveDraft();
        if (!draftId.value) return;
    }
    formMsg.value = "发布中...";
    try {
        const response = await apiFetch(`/posts/${draftId.value}/publish`, { method: "POST" });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (response.status === 403) await logoutAuthor();
            throw new Error(body.message ?? `HTTP ${response.status}`);
        }
        const created = await response.json();
        draftId.value = null;
        draft.title = "";
        draft.content = "";
        editPublished.value = false;
        editingRouteId.value = null;
        if (editorRef.value) editorRef.value.innerHTML = "";
        formMsg.value = "发布成功。";
        deleteMode.value = false;
        editMode.value = false;
        await loadPosts();
        goToDetail(created.id);
    } catch (error) {
        formMsg.value = `发布失败: ${error.message}`;
    }
}

async function loadPostDetail(id) {
    detailLoading.value = true;
    detailErrorMsg.value = "";
    try {
        const response = await apiFetch(`/posts/${id}`);
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        detailPost.value = await response.json();
    } catch (error) {
        detailErrorMsg.value = `详情加载失败: ${error.message}`;
    } finally {
        detailLoading.value = false;
    }
}

function openAuthorModal() {
    authorActionModalVisible.value = false;
    authorModalVisible.value = true;
    authorMsg.value = "";
}

function onAuthorAction() {
    if (isAuthor.value) {
        authorActionModalVisible.value = true;
        return;
    }
    openAuthorModal();
}

function closeAuthorModal() {
    authorModalVisible.value = false;
    authorMsg.value = "";
}

async function verifyAuthor() {
    if (!authorCandidate.value) {
        authorMsg.value = "请输入密码。";
        return;
    }
    authorMsg.value = "验证中...";
    try {
        const response = await apiFetch("/posts/author/verify", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ password: authorCandidate.value }),
        });
        if (!response.ok) {
            const hint = response.status === 429
                ? "尝试次数过多，请约 1 分钟后再试。"
                : (await response.json().catch(() => ({}))).message ?? "密码错误。";
            throw new Error(hint);
        }
        isAuthor.value = true;
        deleteMode.value = false;
        editMode.value = false;
        authorCandidate.value = "";
        authorMsg.value = "作者模式已开启。";
        closeAuthorModal();
        authorActionModalVisible.value = true;
    } catch (error) {
        authorMsg.value = error.message;
    }
}

async function logoutAuthor() {
    try {
        await apiFetch("/posts/author/logout", { method: "POST" });
    } catch (_error) {}
    isAuthor.value = false;
    deleteMode.value = false;
    editMode.value = false;
    editPublished.value = false;
    editingRouteId.value = null;
    authorActionModalVisible.value = false;
    if (currentView.value === "write") goBackToList();
}

function goToDetail(id) {
    window.location.hash = `#/posts/${id}`;
}

function goBackToList() {
    window.location.hash = "";
}

function goToWrite() {
    if (!isAuthor.value) {
        openAuthorModal();
        return;
    }
    window.location.hash = "#/write";
}

async function loadPostForEditRoute(id) {
    if (!isAuthor.value) return;
    draftLoading.value = true;
    formMsg.value = "加载文章...";
    try {
        const response = await apiFetch(`/posts/author/${id}`);
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (response.status === 403) await logoutAuthor();
            throw new Error(body.message ?? `HTTP ${response.status}`);
        }
        const data = await response.json();
        suppressAutoSave.value = true;
        editingRouteId.value = id;
        draftId.value = data.id ?? id;
        editPublished.value = Boolean(data.published);
        draft.title = data.title ?? "";
        draft.content = data.content ?? "";
        await nextTick();
        if (editorRef.value) {
            editorRef.value.innerHTML = draft.content || "";
            syncEditorContent();
        }
        formMsg.value = editPublished.value
            ? "正在编辑已发布文章，修改可通过保存或自动保存同步。"
            : "正在编辑草稿，完成后可发布文章。";
    } catch (error) {
        formMsg.value = `加载失败: ${error.message}`;
        window.location.hash = "";
    } finally {
        setTimeout(() => { suppressAutoSave.value = false; }, 0);
        draftLoading.value = false;
    }
}

function editPost(id) {
    if (!isAuthor.value) {
        errorMsg.value = "请先验证作者身份。";
        return;
    }
    editMode.value = false;
    authorActionModalVisible.value = false;
    window.location.hash = `#/edit/${id}`;
}

function syncRouteFromHash() {
    const editMatched = window.location.hash.match(/^#\/edit\/(\d+)$/);
    if (editMatched) {
        if (!isAuthor.value) {
            openAuthorModal();
            window.location.hash = "";
            return;
        }
        const id = Number(editMatched[1]);
        if (Number.isNaN(id)) {
            window.location.hash = "";
            return;
        }
        currentView.value = "write";
        currentPostId.value = null;
        detailPost.value = null;
        detailErrorMsg.value = "";
        loadPostForEditRoute(id);
        return;
    }

    if (window.location.hash === "#/write") {
        if (!isAuthor.value) {
            openAuthorModal();
            window.location.hash = "";
            return;
        }
        currentView.value = "write";
        currentPostId.value = null;
        detailPost.value = null;
        detailErrorMsg.value = "";
        editingRouteId.value = null;
        editPublished.value = false;
        loadLatestDraft(true);
        return;
    }

    const matched = window.location.hash.match(/^#\/posts\/(\d+)$/);
    if (!matched) {
        currentView.value = "list";
        currentPostId.value = null;
        detailPost.value = null;
        detailErrorMsg.value = "";
        editingRouteId.value = null;
        editPublished.value = false;
        return;
    }

    const id = Number(matched[1]);
    if (!Number.isNaN(id)) {
        currentView.value = "detail";
        currentPostId.value = id;
        loadPostDetail(id);
    }
}

onMounted(() => {
    loadPosts();
    refreshAuthorStatus().then(syncRouteFromHash);
    window.addEventListener("hashchange", syncRouteFromHash);
});

onBeforeUnmount(() => {
    window.removeEventListener("hashchange", syncRouteFromHash);
    clearAutoSaveTimer();
});

function applyCommand(command, value = null) {
    if (!editorRef.value) return;
    editorRef.value.focus();
    document.execCommand(command, false, value);
    syncEditorContent();
}

function setParagraph(tagName) {
    applyCommand("formatBlock", (tagName || "P").toLowerCase());
}

function changeFontSize() {
    applyCommand("fontSize", fontSizeRef.value);
}

function syncEditorContent() {
    draft.content = editorRef.value ? editorRef.value.innerHTML : "";
}

function selectImage() {
    if (!isAuthor.value) {
        formMsg.value = "请先验证作者身份后再上传图片。";
        return;
    }
    imageInputRef.value?.click();
}

function insertLink() {
    if (!editorRef.value) return;
    const rawUrl = window.prompt("请输入链接地址（例如 https://example.com）");
    if (!rawUrl) return;
    const url = rawUrl.trim();
    if (!url) return;
    const selectedText = window.getSelection?.()?.toString().trim() || "";
    editorRef.value.focus();
    if (selectedText) {
        document.execCommand("createLink", false, url);
    } else {
        const linkText = window.prompt("请输入链接显示文字", url) || url;
        document.execCommand(
            "insertHTML", false,
            `<a href="${url}" target="_blank" rel="noopener noreferrer">${linkText}</a>`,
        );
    }
    syncEditorContent();
}

function insertRemoteImage() {
    if (!editorRef.value) return;
    const rawUrl = window.prompt("请输入图片链接地址");
    if (!rawUrl) return;
    const url = rawUrl.trim();
    if (!url) return;
    applyCommand("insertImage", url);
}

function insertTable() {
    if (!editorRef.value) return;
    const rows = Number(window.prompt("表格行数（1-10）", "3"));
    const cols = Number(window.prompt("表格列数（1-8）", "3"));
    if (!Number.isInteger(rows) || !Number.isInteger(cols) || rows < 1 || cols < 1 || rows > 10 || cols > 8) {
        formMsg.value = "表格尺寸无效，请输入合理的行列数。";
        return;
    }
    const headCells = Array.from({ length: cols }, (_, i) => `<th>标题${i + 1}</th>`).join("");
    const bodyRows = Array.from({ length: rows - 1 }, () => {
        const rowCells = Array.from({ length: cols }, () => "<td>内容</td>").join("");
        return `<tr>${rowCells}</tr>`;
    }).join("");
    editorRef.value.focus();
    document.execCommand(
        "insertHTML", false,
        `<table><thead><tr>${headCells}</tr></thead><tbody>${bodyRows || `<tr>${Array.from({ length: cols }, () => "<td>内容</td>").join("")}</tr>`}</tbody></table><p></p>`,
    );
    syncEditorContent();
}

async function uploadImage(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append("image", file);
    formMsg.value = "图片上传中...";
    try {
        const response = await apiFetch("/uploads/images", { method: "POST", body: formData });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (response.status === 403) await logoutAuthor();
            throw new Error(body.message ?? `HTTP ${response.status}`);
        }
        const result = await response.json();
        applyCommand("insertImage", result.url);
        formMsg.value = "图片已插入正文。";
    } catch (error) {
        formMsg.value = `图片上传失败: ${error.message}`;
    } finally {
        event.target.value = "";
    }
}

function closeAuthorActionModal() {
    authorActionModalVisible.value = false;
}

function openPublishMode() {
    deleteMode.value = false;
    editMode.value = false;
    authorActionModalVisible.value = false;
    goToWrite();
}

function openEditMode() {
    editMode.value = true;
    deleteMode.value = false;
    authorActionModalVisible.value = false;
    goBackToList();
}

function openDeleteMode() {
    deleteMode.value = true;
    editMode.value = false;
    authorActionModalVisible.value = false;
    goBackToList();
}

async function deletePost(id) {
    if (!isAuthor.value) {
        errorMsg.value = "请先验证作者身份。";
        return;
    }
    if (!window.confirm("确定要删除这篇帖子吗？删除后无法恢复。")) return;
    try {
        const response = await apiFetch(`/posts/${id}`, { method: "DELETE" });
        if (!response.ok) {
            const body = await response.json().catch(() => ({}));
            if (response.status === 403) await logoutAuthor();
            throw new Error(body.message ?? `HTTP ${response.status}`);
        }
        await loadPosts();
        if (currentPostId.value === id) goBackToList();
    } catch (error) {
        errorMsg.value = `删除失败: ${error.message}`;
    }
}

async function refreshAuthorStatus() {
    try {
        const response = await apiFetch("/posts/author/status");
        if (!response.ok) {
            isAuthor.value = false;
            return;
        }
        const body = await response.json();
        isAuthor.value = Boolean(body.authenticated);
        if (!isAuthor.value) {
            deleteMode.value = false;
            editMode.value = false;
        }
    } catch (_error) {
        isAuthor.value = false;
        deleteMode.value = false;
        editMode.value = false;
    }
}

async function loadLatestDraft(force = false) {
    if (!isAuthor.value) return;
    if (draftLoading.value) return;
    if (!force && draftId.value) return;
    if (!force && (draft.title.trim() || draft.content.trim())) return;
    draftLoading.value = true;
    try {
        const response = await apiFetch("/posts/drafts/latest");
        if (!response.ok) {
            if (force && response.status !== 404) {
                const body = await response.json().catch(() => ({}));
                formMsg.value = body.message ?? `加载草稿失败: HTTP ${response.status}`;
            }
            return;
        }
        const latest = await response.json();
        if (!latest?.id) return;
        suppressAutoSave.value = true;
        draftId.value = latest.id;
        draft.title = latest.title ?? "";
        draft.content = latest.content ?? "";
        await nextTick();
        if (editorRef.value) {
            editorRef.value.innerHTML = draft.content || "";
            syncEditorContent();
        }
        formMsg.value = "已加载最近保存的草稿。";
    } catch (_error) {
        if (force) formMsg.value = "加载草稿失败，请稍后重试。";
    } finally {
        setTimeout(() => { suppressAutoSave.value = false; }, 0);
        draftLoading.value = false;
    }
}

function clearAutoSaveTimer() {
    if (autoSaveTimer.value) {
        clearTimeout(autoSaveTimer.value);
        autoSaveTimer.value = null;
    }
}

function scheduleAutoSave() {
    clearAutoSaveTimer();
    autoSaveTimer.value = setTimeout(() => { persistDraft(true); }, 1200);
}

watch(
    () => [draft.title, draft.content],
    () => {
        if (suppressAutoSave.value) return;
        if (!isAuthor.value) return;
        if (currentView.value !== "write") return;
        if (!draft.title.trim()) return;
        scheduleAutoSave();
    },
);
</script>
