<template>
  <div class="layout">
    <header class="topbar">
      <div class="topbar-inner">
        <div>
          <h1>Earth丿的个人博客</h1>
        </div>
        <div class="topbar-actions">
          <button type="button" @click="openAuthorModal">作者模式</button>
          <button
            v-if="authorPassword"
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
      <Transition name="panel-fade-slide">
        <section v-if="authorPassword" class="card">
          <h3>新增帖子</h3>
          <form @submit.prevent="submitPost">
            <label>
              标题
              <input v-model.trim="draft.title" maxlength="200" required />
            </label>
            <label>内容</label>
            <div class="editor-toolbar">
              <select v-model="fontSizeRef" @change="changeFontSize">
                <option value="1">字号 12</option>
                <option value="2">字号 14</option>
                <option value="3">字号 16</option>
                <option value="4">字号 18</option>
                <option value="5">字号 24</option>
              </select>
              <button type="button" class="secondary" @click="applyCommand('bold')">加粗</button>
              <button type="button" class="secondary" @click="applyCommand('italic')">斜体</button>
              <button type="button" class="secondary" @click="selectImage">上传图片</button>
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
            <button type="submit">发布</button>
          </form>
          <p class="muted">{{ formMsg }}</p>
        </section>
      </Transition>

      <section class="card">
        <div class="list-header">
          <h3 v-if="!currentPostId">文章列表</h3>
          <h3 v-else>文章详情</h3>
          <div class="list-tools">
            <span class="muted">{{ postCountLabel }}</span>
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
            <button
              v-for="post in posts"
              :key="post.id"
              type="button"
              class="post-list-item"
              @click="goToDetail(post.id)"
            >
              <span class="post-title">{{ post.title }}</span>
            </button>
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
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

const API_BASE = import.meta.env.VITE_API_BASE || "/api";

const posts = ref([]);
const loading = ref(false);
const errorMsg = ref("");
const currentPostId = ref(null);
const detailPost = ref(null);
const detailLoading = ref(false);
const detailErrorMsg = ref("");
const editorRef = ref(null);
const imageInputRef = ref(null);
const fontSizeRef = ref("3");

const authorPassword = ref("");
const authorCandidate = ref("");
const authorMsg = ref("");
const authorModalVisible = ref(false);

const formMsg = ref("");
const draft = reactive({
  title: "",
  content: "",
});

const postCountLabel = computed(() => `${posts.value.length} 篇`);

function apiUrl(path) {
  return `${API_BASE}${path}`;
}

async function loadPosts() {
  loading.value = true;
  errorMsg.value = "";
  try {
    const response = await fetch(apiUrl("/posts"));
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

async function submitPost() {
  if (!authorPassword.value) {
    formMsg.value = "请先点击作者按钮并验证密码。";
    return;
  }
  formMsg.value = "提交中...";
  try {
    const response = await fetch(apiUrl("/posts"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Author-Password": authorPassword.value,
      },
      body: JSON.stringify(draft),
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      if (response.status === 403) {
        logoutAuthor();
      }
      throw new Error(body.message ?? `HTTP ${response.status}`);
    }
    const created = await response.json();
    draft.title = "";
    draft.content = "";
    if (editorRef.value) {
      editorRef.value.innerHTML = "";
    }
    formMsg.value = "发布成功。";
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
    const response = await fetch(apiUrl(`/posts/${id}`));
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
  authorModalVisible.value = true;
  authorMsg.value = "";
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
    const response = await fetch(apiUrl("/posts/author/verify"), {
      method: "POST",
      headers: {
        "X-Author-Password": authorCandidate.value,
      },
    });
    if (!response.ok) {
      throw new Error("密码错误。");
    }
    authorPassword.value = authorCandidate.value;
    authorCandidate.value = "";
    authorMsg.value = "作者模式已开启。";
    setTimeout(closeAuthorModal, 200);
  } catch (error) {
    authorMsg.value = error.message;
  }
}

function logoutAuthor() {
  authorPassword.value = "";
}

function goToDetail(id) {
  window.location.hash = `#/posts/${id}`;
}

function goBackToList() {
  window.location.hash = "#/";
}

function syncRouteFromHash() {
  const matched = window.location.hash.match(/^#\/posts\/(\d+)$/);
  if (!matched) {
    currentPostId.value = null;
    detailPost.value = null;
    detailErrorMsg.value = "";
    return;
  }

  const id = Number(matched[1]);
  if (!Number.isNaN(id)) {
    currentPostId.value = id;
    loadPostDetail(id);
  }
}

onMounted(() => {
  loadPosts();
  syncRouteFromHash();
  window.addEventListener("hashchange", syncRouteFromHash);
});

onBeforeUnmount(() => {
  window.removeEventListener("hashchange", syncRouteFromHash);
});

function applyCommand(command, value = null) {
  if (!editorRef.value) return;
  editorRef.value.focus();
  document.execCommand(command, false, value);
  syncEditorContent();
}

function changeFontSize() {
  applyCommand("fontSize", fontSizeRef.value);
}

function syncEditorContent() {
  draft.content = editorRef.value ? editorRef.value.innerHTML : "";
}

function selectImage() {
  if (!authorPassword.value) {
    formMsg.value = "请先验证作者身份后再上传图片。";
    return;
  }
  imageInputRef.value?.click();
}

async function uploadImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;

  const formData = new FormData();
  formData.append("image", file);

  formMsg.value = "图片上传中...";
  try {
    const response = await fetch(apiUrl("/uploads/images"), {
      method: "POST",
      headers: {
        "X-Author-Password": authorPassword.value,
      },
      body: formData,
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
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
</script>
