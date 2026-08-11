<script setup>

import {ref, nextTick} from 'vue'
import axios from 'axios'
import {Check, Copy} from 'lucide-vue-next'
import VoiceButton from './VoiceButton.vue'
import FileUpload from './FileUpload.vue'


const input = ref('')
const messages = ref([])
const loading = ref(false)
const isComposing = ref(false)
const chatArea = ref(null)
const inputArea = ref(null)
const copiedMessageIndex = ref(null)
let copyTimer = null
const conversationId = ref(
    crypto.randomUUID()
)

async function send(selectedRoutes = []) {
  if (loading.value || !input.value.trim()) return

  const question = input.value
  messages.value.push({ role: 'user', text: question })
  scrollToBottom()
  input.value = ''
  nextTick(autoResizeInput)
  loading.value = true
  scrollToBottom()

  try {
    const routes = (Array.isArray(selectedRoutes) ? selectedRoutes : [selectedRoutes])
        .map(route => typeof route === 'string' ? route : route?.id || route?.route)
        .filter(Boolean)

    const res = await axios.post('/api/rag/question', {
      question,
      conversationId: conversationId.value,
      selectedRoutes: routes
    })

    const data = res.data.data
    if (data.status === 'NEED_CONFIRMATION') {
      messages.value.push({
        role: 'ai',
        text: data.question,
        options: data.options,
        originalQuestion: question
      })
    } else {
      messages.value.push({ role: 'ai', text: data.answer })
    }
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

function chooseRoute(message, option) {
  const route = typeof option === 'string' ? option : option?.id || option?.route
  if (!route) return
  send([route]).then(() => {
    message.options = []
  })
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

function autoResizeInput() {
  const textarea = inputArea.value
  if (!textarea) return

  textarea.style.height = 'auto'
  const maxHeight = 240
  textarea.style.height = `${Math.min(textarea.scrollHeight, maxHeight)}px`
  textarea.style.overflowY = textarea.scrollHeight > maxHeight ? 'auto' : 'hidden'
}

function handleCompositionStart() {
  isComposing.value = true
}

function handleCompositionEnd() {
  isComposing.value = false
  nextTick(autoResizeInput)
}

function handleInputEnter(event) {
  // 한글 IME 조합 중 Enter는 전송이 아니라 현재 글자 조합 완료에 사용한다.
  if (isComposing.value || event.isComposing || event.keyCode === 229) return
  event.preventDefault()
  send()
}

async function copyMessage(message, index) {
  const text = message.text || ''
  if (!text) return

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      textarea.remove()
    }

    copiedMessageIndex.value = index
    clearTimeout(copyTimer)
    copyTimer = setTimeout(() => {
      copiedMessageIndex.value = null
    }, 2000)
  } catch (error) {
    console.error('메시지 복사 실패', error)
  }
}

</script>


<template>

  <div class="relative w-screen h-screen overflow-hidden bg-white">


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


          <!-- 질문 표시 -->
          <div
              v-if="message.role==='user'"
              class="
                    text-left
                    text-xs
                    text-gray-500
                    mb-1
                    "
          >
            나
          </div>

          <div v-if="message.options?.length" class="mt-3 flex flex-wrap gap-2">
            <button
                v-for="option in message.options"
                :key="option.id"
                class="rounded-full px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 hover:font-bold"
                @click="chooseRoute(message, option)"
            >
              {{ option.label }}
            </button>
          </div>


          <!-- AI 표시 -->
          <div
              v-if="message.role !== 'user'"
              class="
                    text-left
                    text-xs
                    text-gray-500
                    mb-1
                    "
          >
            시스템
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

          <!-- 질문·답변 복사 버튼 -->
          <div
              class="mt-1 flex"
              :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <button
                type="button"
                class="copy-message-button"
                :aria-label="copiedMessageIndex === index ? '복사 완료' : '메시지 복사'"
                :title="copiedMessageIndex === index ? '복사 완료' : '복사'"
                @click="copyMessage(message, index)"
            >
              <Check v-if="copiedMessageIndex === index" :size="15" stroke-width="2.5" />
              <Copy v-else :size="15" stroke-width="2" />
            </button>
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
            items-end
            gap-3
            bg-white
            border border-gray-200
            rounded-3xl
            px-4
            py-2
            "
      >


        <FileUpload/>


        <textarea
            ref="inputArea"
            v-model="input"
            @keydown.enter.exact="handleInputEnter"
            @compositionstart="handleCompositionStart"
            @compositionend="handleCompositionEnd"
            @input="autoResizeInput"
            placeholder="입력하세요"
            rows="1"
            class="
                flex-1
                outline-none
                py-3
                resize-none
                input-textarea
                "
        ></textarea>


        <VoiceButton

            @update="voiceTyping"
            @submit="sendVoice"
        />


        <button
            type="button"
            :disabled="loading"
            @click="send"
            class="
                soft-action-button
                w-10
                h-10
                rounded-full
                "
        >

          ➤

        </button>


      </div>


    </footer>


  </div>


</template>
