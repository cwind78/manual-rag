<script setup>
import { ref } from 'vue'
import { Mic } from 'lucide-vue-next'

const emit = defineEmits([
  'update',
  'submit'
])

const listening = ref(false)

let recognition = null

function start() {

  const SpeechRecognition =
      window.SpeechRecognition ||
      window.webkitSpeechRecognition

  if (!SpeechRecognition) {
    alert("이 브라우저는 음성 인식을 지원하지 않습니다.")
    return
  }

  recognition = new SpeechRecognition()

  recognition.lang = "ko-KR"

  recognition.interimResults = true

  recognition.continuous = false

  recognition.maxAlternatives = 1

  listening.value = true

  recognition.onaudiostart = () => {
    console.log("audio start")
  }


  recognition.onsoundstart = () => {
    console.log("sound start")
  }


  recognition.onspeechstart = () => {
    console.log("speech start")
  }

  recognition.onresult = (event) => {
consloe.log(event)
    let text = ""

    for (let i = 0; i < event.results.length; i++) {
      text += event.results[i][0].transcript
    }

    emit("update", text)
  }

  recognition.onspeechend = () => {
    recognition.stop()
  }

  recognition.onend = () => {
    console.log("end")
    listening.value = false
    emit("submit")
  }

  recognition.onerror = (e) => {
    console.log(e);
    listening.value = false
  }

  recognition.start = () => {
    console.log("start")
  }

  recognition.start()
}
</script>

<template>

  <button
      @click="start"
      class="
    w-9
    h-9
    rounded-full
    flex
    items-center
    justify-center
    transition
    "
      :class="listening ? 'bg-red-500 text-white' : 'hover:bg-gray-100'"
  >

    <Mic :size="18"/>

  </button>

</template>