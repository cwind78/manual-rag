<script setup>
import { reactive, ref } from "vue"
import axios from "axios"


const showModal = ref(false)

const categories = [
  "사용자 설명서",
  "서비스 매뉴얼",
  "설치 가이드",
  "기술 문서",
  "용어집",
  "FAQ",
  "문학",
  "기타"
]


const form = reactive({
  file: null,
  title: "",
  category: "",
  metadata: [
    {
      key: "",
      value: ""
    }
  ]
})


function openUploadModal() {
  showModal.value = true
}


function closeModal() {
  showModal.value = false

  form.file = null
  form.title = ""
  form.category = ""

  form.metadata = [
    {
      key: "",
      value: ""
    }
  ]
}


function selectFile(event) {

  const file = event.target.files[0]

  if (!file) {
    return
  }

  form.file = file

  // 파일명을 제목 기본값으로 설정
  if (!form.title) {
    form.title = file.name.replace(/\.[^/.]+$/, "")
  }
}


function addMetadata() {

  form.metadata.push({
    key: "",
    value: ""
  })

}


function removeMetadata(index) {

  form.metadata.splice(index, 1)

}


async function upload() {

  if (!form.file) {
    alert("파일을 선택하세요.")
    return
  }


  if (!form.title) {
    alert("제목을 입력하세요.")
    return
  }


  if (!form.category) {
    alert("카테고리를 선택하세요.")
    return
  }


  const metadata = {}

  form.metadata.forEach(item => {

    if (item.key.trim()) {
      metadata[item.key] = item.value
    }

  })


  const request = {
    title: form.title,
    category: form.category,
    metadata: metadata
  }


  const formData = new FormData()


  formData.append(
      "file",
      form.file
  )


  formData.append(
      "request",
      new Blob(
          [
            JSON.stringify(request)
          ],
          {
            type: "application/json"
          }
      )
  )


  try {

    await axios.post(
        "/api/documents/upload",
        formData
    )


    alert("업로드 완료")

    closeModal()

  } catch (e) {

    console.error(e)

    alert("업로드 실패")

  }

}

</script>
<template>

  <!-- 업로드 버튼 -->
  <button
      class="
        cursor-pointer
        text-sm
        text-gray-600
        hover:text-blue-600
      "
      @click="openUploadModal"
  >
    📎 업로드
  </button>


  <!-- Modal -->
  <div
      v-if="showModal"
      class="
        fixed
        inset-0
        bg-black/40
        flex
        items-center
        justify-center
        z-50
      "
  >

    <div
        class="
          bg-white
          rounded-xl
          shadow-xl
          w-[500px]
          p-6
        "
    >

      <h2
          class="
            text-lg
            font-bold
            mb-5
          "
      >
        문서 업로드
      </h2>


      <!-- 파일 -->
      <div class="mb-4">

        <label
            class="
              block
              text-sm
              font-medium
              mb-1
            "
        >
          파일
        </label>


        <input
            type="file"
            @change="selectFile"
            class="
              w-full
              border
              rounded
              p-2
            "
        />


        <div
            v-if="form.file"
            class="
              text-xs
              text-gray-500
              mt-1
            "
        >
          {{ form.file.name }}
        </div>

      </div>



      <!-- 제목 -->
      <div class="mb-4">

        <label
            class="
              block
              text-sm
              font-medium
              mb-1
            "
        >
          제목
        </label>


        <input
            v-model="form.title"
            placeholder="예) 모델코드 사용자 설명서"
            class="
              w-full
              border
              rounded
              p-2
            "
        />

      </div>




      <!-- 카테고리 -->
      <div class="mb-4">

        <label
            class="
              block
              text-sm
              font-medium
              mb-1
            "
        >
          카테고리
        </label>


        <select
            v-model="form.category"
            class="
              w-full
              border
              rounded
              p-2
            "
        >

          <option
              value=""
          >
            선택하세요
          </option>


          <option
              v-for="category in categories"
              :key="category"
              :value="category"
          >
            {{ category }}
          </option>

        </select>

      </div>




      <!-- 메타데이터 -->
      <div class="mb-4">


        <div
            class="
              flex
              justify-between
              items-center
              mb-2
            "
        >

          <label
              class="
                text-sm
                font-medium
              "
          >
            메타데이터
          </label>


          <button
              type="button"
              @click="addMetadata"
              class="
                text-xs
                text-blue-600
              "
          >
            + 추가
          </button>


        </div>




        <div
            v-for="(item,index) in form.metadata"
            :key="index"
            class="
              flex
              gap-2
              mb-2
            "
        >

          <input
              v-model="item.key"
              placeholder="키"
              class="
                flex-1
                border
                rounded
                p-2
                text-sm
              "
          />


          <input
              v-model="item.value"
              placeholder="값"
              class="
                flex-1
                border
                rounded
                p-2
                text-sm
              "
          />


          <button
              type="button"
              @click="removeMetadata(index)"
              class="
                text-red-500
                text-sm
              "
          >
            삭제
          </button>


        </div>


      </div>





      <!-- 버튼 -->
      <div
          class="
            flex
            justify-end
            gap-2
            mt-6
          "
      >

        <button
            type="button"
            @click="closeModal"
            class="
              px-4
              py-2
              border
              rounded
            "
        >
          취소
        </button>


        <button
            type="button"
            @click="upload"
            class="
              px-4
              py-2
              bg-blue-600
              text-white
              rounded
            "
        >
          업로드
        </button>


      </div>


    </div>

  </div>


</template>