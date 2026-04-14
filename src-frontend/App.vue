<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar-inner">
        <div>
          <h1>Earth丿的个人博客</h1>
        </div>
        <div class="topbar-actions">
          <button type="button" @click="onAuthorAction">
            {{ isAuthor ? "作者操作" : "作者模式" }}
          </button>
          <button
            v-if="isAuthor"
            type="button"
            class="secondary"
            @click="logoutAuthor"
          >
            退出
          </button>
        </div>
      </div>
    </header>

    <main class="container">
      <Transition name="view-switch" mode="out-in">
        <section v-if="currentView === 'write'" key="compose-view" class="card compose-card">
          <div class="compose-header">
            <div>
              <h3>创作新文章</h3>
              <p class="muted">记录你的想法，支持超链接、图片、表格与多种排版能力。</p>
            </div>
            <button type="button" class="secondary" @click="goBackToList">返回列表</button>
          </div>
          <form class="compose-form" @submit.prevent="publishPost">
            <label>
              标题
              <input
                v-model.trim="draft.title"
                maxlength="200"
                placeholder="给文章起一个醒目的标题"
                required
              />
            </label>
            <label>正文</label>
            <div class="editor-toolbar">
              <select v-model="fontSizeRef" @change="changeFontSize">
                <option value="1">字号 12</option>
                <option value="2">字号 14</option>
                <option value="3">字号 16</option>
                <option value="4">字号 18</option>
                <option value="5">字号 24</option>
              </select>
              <button type="button" class="secondary" @click="setParagraph('P')">正文</button>
              <button type="button" class="secondary" @click="setParagraph('H2')">H2</button>
              <button type="button" class="secondary" @click="setParagraph('H3')">H3</button>
              <button type="button" class="secondary" @click="applyCommand('bold')">加粗</button>
              <button type="button" class="secondary" @click="applyCommand('italic')">斜体</button>
              <button type="button" class="secondary" @click="applyCommand('underline')">下划线</button>
              <button type="button" class="secondary" @click="applyCommand('strikeThrough')">删除线</button>
              <button type="button" class="secondary" @click="applyCommand('insertUnorderedList')">无序列表</button>
              <button type="button" class="secondary" @click="applyCommand('insertOrderedList')">有序列表</button>
              <button type="button" class="secondary" @click="applyCommand('formatBlock', 'blockquote')">引用</button>
              <button type="button" class="secondary" @click="applyCommand('insertHorizontalRule')">分割线</button>
              <button type="button" class="secondary" @click="insertLink">超链接</button>
              <button type="button" class="secondary" @click="applyCommand('unlink')">取消链接</button>
              <button type="button" class="secondary" @click="insertRemoteImage">网络图</button>
              <button type="button" class="secondary" @click="selectImage">上传图片</button>
              <button type="button" class="secondary" @click="insertTable">插入表格</button>
              <input
                ref="imageInputRef"
                type="file"
                accept="image/*"
                class="hidden-input"
                @change="uploadImage"
              />
            </div>
            <div
              ref="editorRef"
              class="rich-editor"
              contenteditable="true"
              @input="syncEditorContent"
            ></div>
            <div class="compose-actions">
              <button type="button" class="secondary" @click="loadLatestDraft(true)">加载草稿</button>
              <button type="button" class="secondary" @click="saveDraft">保存草稿</button>
              <button type="submit">发布文章</button>
              <span class="muted">{{ formMsg }}</span>
            </div>
          </form>
        </section>

        <section v-else key="reading-view" class="card">
          <div class="list-header">
            <h3 v-if="!currentPostId">文章列表</h3>
            <h3 v-else>文章详情</h3>
            <div class="list-tools">
              <span class="muted">{{ postCountLabel }}</span>
              <span v-if="deleteMode && !currentPostId" class="muted">删除模式</span>
              <button
                v-if="currentPostId"
                type="button"
                class="secondary"
                @click="goBackToList"
              >
                返回列表
              </button>
              <button type="button" class="secondary" @click="loadPosts">刷新</button>
            </div>
          </div>
          <Transition name="view-switch" mode="out-in">
            <TransitionGroup v-if="!currentPostId" key="post-list" id="posts" name="post-list" tag="div">
              <p v-if="loading" key="loading" class="muted">加载中...</p>
              <p v-else-if="errorMsg" key="error" class="muted">{{ errorMsg }}</p>
              <p v-else-if="posts.length === 0" key="empty" class="muted">还没有文章，先发布第一篇吧。</p>
              <div
                v-for="post in posts"
                :key="post.id"
                class="post-list-row"
              >
                <button
                  type="button"
                  class="post-list-item"
                  @click="goToDetail(post.id)"
                >
                  <span class="post-title">{{ post.title }}</span>
                </button>
                <button
                  v-if="deleteMode && isAuthor"
                  type="button"
                  class="danger"
                  @click="deletePost(post.id)"
                >
                  删除帖子
                </button>
              </div>
            </TransitionGroup>
            <div v-else key="post-detail" id="post-detail">
              <p v-if="detailLoading" class="muted">详情加载中...</p>
              <p v-else-if="detailErrorMsg" class="muted">{{ detailErrorMsg }}</p>
              <article v-else-if="detailPost" class="post-item">
                <h4>{{ detailPost.title }}</h4>
                <div class="post-content" v-html="detailPost.content"></div>
              </article>
            </div>
          </Transition>
        </section>
      </Transition>
    </main>

    <Transition name="modal-fade">
      <div v-if="authorModalVisible" class="modal">
        <div class="modal-mask" @click="closeAuthorModal"></div>
        <div class="modal-panel">
          <h3>作者验证</h3>
          <p class="muted">通过密码验证后可发布帖子</p>
          <label>
            作者密码
            <input
              v-model.trim="authorCandidate"
              type="password"
              placeholder="请输入作者密码"
              @keydown.enter="verifyAuthor"
            />
          </label>
          <div class="modal-actions">
            <button type="button" @click="verifyAuthor">验证并进入</button>
            <button type="button" class="secondary" @click="closeAuthorModal">取消</button>
          </div>
          <p class="muted">{{ authorMsg }}</p>
        </div>
      </div>
    </Transition>

    <Transition name="modal-fade">
      <div v-if="authorActionModalVisible" class="modal">
        <div class="modal-mask" @click="closeAuthorActionModal"></div>
        <div class="modal-panel">
          <h3>作者操作</h3>
          <p class="muted">请选择你要执行的操作。</p>
          <div class="modal-actions">
            <button type="button" @click="openPublishMode">发布帖子</button>
            <button type="button" class="danger" @click="openDeleteMode">删除帖子</button>
            <button type="button" class="secondary" @click="closeAuthorActionModal">取消</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";

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

/** Session cookie (JSESSIONID) must be sent after author login; default fetch omits it on some cross-site cases. */
function apiFetch(path, init = {}) {
  return fetch(apiUrl(path), { ...init, credentials: "include" });
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
      formMsg.value = "请先点击作者按钮并验证密码。";
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
    formMsg.value = "草稿保存中...";
  }
  try {
    const response = await apiFetch("/posts/drafts", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        id: draftId.value,
        title: draft.title,
        content: draft.content,
      }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      if (response.status === 403) {
        await logoutAuthor();
      }
      throw new Error(body.message ?? `HTTP ${response.status}`);
    }
    const saved = await response.json();
    draftId.value = saved.id ?? draftId.value;
    formMsg.value = silent ? `草稿自动保存于 ${new Date().toLocaleTimeString()}` : "草稿已保存到数据库（未发布）。";
  } catch (error) {
    formMsg.value = silent ? `自动保存失败: ${error.message}` : `保存草稿失败: ${error.message}`;
  } finally {
    autoSaving.value = false;
  }
}

async function publishPost() {
  if (!isAuthor.value) {
    formMsg.value = "请先点击作者按钮并验证密码。";
    return;
  }
  if (!draft.title.trim()) {
    formMsg.value = "请先填写标题再发布。";
    return;
  }

  if (!draftId.value) {
    await saveDraft();
    if (!draftId.value) {
      return;
    }
  }

  formMsg.value = "发布中...";
  try {
    const response = await apiFetch(`/posts/${draftId.value}/publish`, {
      method: "POST",
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      if (response.status === 403) {
        await logoutAuthor();
      }
      throw new Error(body.message ?? `HTTP ${response.status}`);
    }
    const created = await response.json();
    draftId.value = null;
    draft.title = "";
    draft.content = "";
    if (editorRef.value) {
      editorRef.value.innerHTML = "";
    }
    formMsg.value = "发布成功。";
    deleteMode.value = false;
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
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
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
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ password: authorCandidate.value }),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      const hint =
        response.status === 429
          ? "尝试次数过多，请约 1 分钟后再试。"
          : body.message ?? "密码错误。";
      throw new Error(hint);
    }
    isAuthor.value = true;
    deleteMode.value = false;
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
    await apiFetch("/posts/author/logout", {
      method: "POST",
    });
  } catch (_error) {
    // Ignore network errors and clear frontend state anyway.
  }
  isAuthor.value = false;
  deleteMode.value = false;
  authorActionModalVisible.value = false;
  if (currentView.value === "write") {
    goBackToList();
  }
}

function goToDetail(id) {
  window.location.hash = `#/posts/${id}`;
}

function goBackToList() {
  window.location.hash = "#/";
}

function goToWrite() {
  if (!isAuthor.value) {
    openAuthorModal();
    return;
  }
  window.location.hash = "#/write";
}

function syncRouteFromHash() {
  if (window.location.hash === "#/write") {
    if (!isAuthor.value) {
      openAuthorModal();
      window.location.hash = "#/";
      return;
    }
    currentView.value = "write";
    currentPostId.value = null;
    detailPost.value = null;
    detailErrorMsg.value = "";
    loadLatestDraft(true);
    return;
  }

  const matched = window.location.hash.match(/^#\/posts\/(\d+)$/);
  if (!matched) {
    currentView.value = "list";
    currentPostId.value = null;
    detailPost.value = null;
    detailErrorMsg.value = "";
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
  const block = (tagName || "P").toLowerCase();
  applyCommand("formatBlock", block);
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
      "insertHTML",
      false,
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

  const headCells = Array.from({ length: cols }, (_, index) => `<th>标题${index + 1}</th>`).join("");
  const bodyRows = Array.from({ length: rows - 1 }, () => {
    const rowCells = Array.from({ length: cols }, () => "<td>内容</td>").join("");
    return `<tr>${rowCells}</tr>`;
  }).join("");

  editorRef.value.focus();
  document.execCommand(
    "insertHTML",
    false,
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
    const response = await apiFetch("/uploads/images", {
      method: "POST",
      body: formData,
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      if (response.status === 403) {
        await logoutAuthor();
      }
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
  authorActionModalVisible.value = false;
  goToWrite();
}

function openDeleteMode() {
  deleteMode.value = true;
  authorActionModalVisible.value = false;
  goBackToList();
}

async function deletePost(id) {
  if (!isAuthor.value) {
    formMsg.value = "请先验证作者身份。";
    return;
  }
  const confirmed = window.confirm("确定要删除这篇帖子吗？删除后无法恢复。");
  if (!confirmed) return;

  try {
    const response = await apiFetch(`/posts/${id}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      if (response.status === 403) {
        await logoutAuthor();
      }
      throw new Error(body.message ?? `HTTP ${response.status}`);
    }
    await loadPosts();
    if (currentPostId.value === id) {
      goBackToList();
    }
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
    }
  } catch (_error) {
    isAuthor.value = false;
    deleteMode.value = false;
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
    if (force) {
      formMsg.value = "加载草稿失败，请稍后重试。";
    }
  } finally {
    setTimeout(() => {
      suppressAutoSave.value = false;
    }, 0);
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
  autoSaveTimer.value = setTimeout(() => {
    persistDraft(true);
  }, 1200);
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
