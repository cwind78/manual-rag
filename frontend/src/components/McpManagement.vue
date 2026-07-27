<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const emit = defineEmits(['close'])

const servers = ref([])
const tools = ref([])
const selected = ref(null)
const form = ref({ name: '', description: '', endpoint: '', authType: 'NONE', accessToken: '' })
const error = ref('')

async function load() {
  const { data } = await axios.get('/api/mcp/servers')
  servers.value = data
}

async function register() {
  error.value = ''
  try {
    await axios.post('/api/mcp/servers', form.value)
    form.value = { name: '', description: '', endpoint: '', authType: 'NONE', accessToken: '' }
    await load()
  } catch (e) { error.value = e.response?.data?.message || 'MCP 등록에 실패했습니다.' }
}

async function selectServer(server) {
  selected.value = server
  const { data } = await axios.get(`/api/mcp/servers/${server.id}/tools`)
  tools.value = data
}

async function refresh() {
  const { data } = await axios.post(`/api/mcp/servers/${selected.value.id}/refresh`)
  tools.value = data
  await load()
}

async function toggle(server) {
  await axios.patch(`/api/mcp/servers/${server.id}/enabled?value=${!server.enabled}`)
  await load()
}

onMounted(load)
</script>

<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4" @click.self="emit('close')">
    <section class="max-h-[calc(100vh-2rem)] w-full max-w-6xl overflow-y-auto rounded-2xl bg-white p-8 text-gray-800 shadow-xl">
      <div class="mx-auto">
        <div class="mb-6 flex items-center justify-between">
          <h1 class="text-2xl font-semibold">Remote MCP 관리</h1>
          <button class="flex h-9 w-9 items-center justify-center rounded-full bg-gray-100 text-xl text-gray-700" aria-label="닫기" @click="emit('close')">×</button>
        </div>
      <div v-if="error" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{{ error }}</div>
      <div class="grid gap-6 lg:grid-cols-[360px_1fr]">
        <form @submit.prevent="register" class="rounded-xl border border-gray-200 p-5">
          <h2 class="mb-4 font-semibold">MCP 등록</h2>
          <input v-model="form.name" required placeholder="이름" class="mb-3 w-full rounded border border-gray-200 bg-white p-2" />
          <input v-model="form.endpoint" required placeholder="https://example.com/mcp" class="mb-3 w-full rounded border border-gray-200 bg-white p-2" />
          <textarea v-model="form.description" placeholder="설명" class="mb-3 w-full rounded border border-gray-200 bg-white p-2" />
          <select v-model="form.authType" class="mb-3 w-full rounded border border-gray-200 bg-white p-2">
            <option value="NONE">인증 없음</option><option value="BEARER">Bearer Token</option>
          </select>
          <input v-if="form.authType === 'BEARER'" v-model="form.accessToken" type="password" placeholder="Access Token" class="mb-3 w-full rounded border border-gray-200 bg-white p-2" />
          <button class="w-full rounded px-4 py-2 text-gray-800 hover:bg-gray-100 hover:font-bold">등록하고 도구 조회</button>
        </form>
        <div>
          <div class="grid gap-3">
            <button v-for="server in servers" :key="server.id" @click="selectServer(server)" class="flex items-center justify-between rounded-xl border border-gray-200 p-4 text-left hover:bg-gray-50">
              <span><b>{{ server.name }}</b><small class="ml-2 text-gray-500">{{ server.endpoint }}</small></span>
              <span class="flex items-center gap-2"><i :class="server.enabled ? 'text-green-600' : 'text-gray-400'">{{ server.enabled ? '연결됨' : '중지됨' }}</i><button @click.stop="toggle(server)" class="rounded px-2 py-1 text-xs text-gray-700 hover:bg-gray-100 hover:font-bold">{{ server.enabled ? '중지' : '활성화' }}</button></span>
            </button>
          </div>
          <div v-if="selected" class="mt-6 rounded-xl border border-gray-200 p-5">
            <div class="mb-4 flex items-center justify-between"><h2 class="font-semibold">{{ selected.name }} 도구</h2><button @click="refresh" class="rounded bg-black px-3 py-1.5 text-sm text-white">새로고침</button></div>
            <div v-if="!tools.length" class="text-sm text-gray-500">등록된 도구가 없습니다.</div>
            <div v-for="tool in tools" :key="tool.id" class="border-t border-gray-100 py-3"><b>{{ tool.name }}</b><p class="text-sm text-gray-500">{{ tool.description }}</p></div>
          </div>
        </div>
      </div>
      </div>
    </section>
  </div>
</template>
