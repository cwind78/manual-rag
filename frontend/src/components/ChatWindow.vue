<script setup>

import {ref, nextTick} from 'vue'
import axios from 'axios'
import VoiceButton from './VoiceButton.vue'
import FileUpload from './FileUpload.vue'


const input = ref('')
const messages = ref([])
const loading = ref(false)
const chatArea = ref(null)
const conversationId = ref(
    crypto.randomUUID()
)

async function send() {
  if (!input.value.trim()) return

  const question = input.value
  messages.value.push({ role: 'user', text: question })
  scrollToBottom()
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await axios.post('/api/rag/question', {
      question,
      conversationId: conversationId.value
    })

    messages.value.push({
      role: 'ai',
      text: res.data.data.answer
    })
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

async function scrollToBottom(){

  await nextTick()

  const el = chatArea.value

  if(!el)
    return


  const target =
      el.scrollHeight - el.clientHeight


  el.scrollTo({
    top: target,
    behavior: 'smooth'
  })

}

function voiceTyping(text) {

  input.value = text

}

function sendVoice() {

  if (input.value.trim()) {
    send()
  }

}


function setVoice(text) {

  input.value = text

}

</script>


<template>

  <div class="relative w-screen h-screen overflow-hidden bg-white">


    <!-- 상단 제목 -->
    <header
        class="
        absolute
        top-0
        left-0
        right-0
        z-10
        flex
        items-center
        px-6
        py-5
        font-semibold
        text-lg
        pointer-events-none
        "
    >

      <span>Manual AI</span>

    </header>


    <!-- 대화 영역 -->
    <main
        ref="chatArea"
        class="
        absolute
        inset-0
        overflow-y-auto
        p-6
        pl-24
        pb-32
        space-y-6
        bg-white
        "
    >


      <div
          v-for="(message,index) in messages"
          :key="index"
          class="flex"
          :class="
                message.role === 'user'
                ? 'justify-end'
                : 'justify-start'
            "
      >


        <div
            class="max-w-3xl"
        >


          <!-- 사용자 표시 -->
          <div
              v-if="message.role==='user'"
              class="
                    text-right
                    text-xs
                    text-gray-500
                    mb-1
                    "
          >
            사용자
          </div>


          <!-- AI 표시 -->
          <div
              v-else
              class="
                    text-left
                    text-xs
                    text-gray-500
                    mb-1
                    "
          >
            Manual AI
          </div>


          <!-- 말풍선 -->
          <div
              class="
                    rounded-2xl
                    px-5
                    py-3
                    whitespace-pre-wrap
                    leading-relaxed
                    "
              :class="
                        message.role==='user'
                        ?
                        'bg-[#f2f2f2] text-gray-900'
                        :
                        'bg-white'
                    "
          >

            {{ message.text }}

          </div>


        </div>


      </div>


      <div
          v-if="loading"
          class="
            flex
            justify-start
            "
      >

        <div
            class="
                bg-white
                rounded-2xl
                px-5
                py-3
                "
        >

          답변 작성 중...

        </div>

      </div>


    </main>


    <!-- 하단 입력 영역 -->
    <footer
        class="
        absolute
        bottom-0
        left-0
        right-0
        z-10
        p-4
        pointer-events-none
        "
    >


      <div
          class="
            max-w-4xl
            mx-auto
            pointer-events-auto
            flex
            items-center
            gap-3
            bg-white
            border border-gray-200
            rounded-3xl
            px-4
            py-2
            "
      >


        <FileUpload/>


        <input
            v-model="input"
            @keyup.enter="send"
            placeholder="입력하세요"
            class="
                flex-1
                outline-none
                py-3
                "
        />


        <VoiceButton

            @update="voiceTyping"
            @submit="sendVoice"
        />


        <button
            @click="send"
            class="
                w-10
                h-10
                rounded-full
                bg-black
                text-white
                "
        >

          ➤

        </button>


      </div>


    </footer>


  </div>


</template>
