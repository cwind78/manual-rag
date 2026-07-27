<script setup>
import ChatWindow from './components/ChatWindow.vue'
import McpManagement from './components/McpManagement.vue'
import { ref } from 'vue'

const showMenu = ref(false)
const showMcp = ref(false)

function openMcp() {
  showMenu.value = false
  showMcp.value = true
}
</script>


<template>
  <main class="relative w-screen h-screen overflow-hidden">
    <button
        class="absolute right-6 top-4 z-40 flex h-10 w-10 items-center justify-center rounded-full text-xl text-gray-800 hover:bg-gray-100"
        aria-label="메뉴 열기"
        @click="showMenu = !showMenu"
    >
      ☰
    </button>
    <ChatWindow />

    <div
        v-if="showMenu"
        class="absolute inset-0 z-30 bg-white/95 p-8 pt-24"
        @click.self="showMenu = false"
    >
      <nav class="mx-auto max-w-5xl">
        <button
            class="menu-item block py-5 text-left text-2xl text-gray-800 hover:font-bold"
            @click="openMcp"
        >
          MCP 관리
        </button>
      </nav>
    </div>

    <McpManagement v-if="showMcp" @close="showMcp = false" />
  </main>
</template>
