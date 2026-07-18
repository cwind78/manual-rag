<script setup>

import axios from 'axios'


async function upload(e){

  const file=e.target.files[0]


  let form=
      new FormData()

  form.append(
      "file",
      file
  )


  await axios.post(
      "/api/document/upload",
      form
  )


  alert("업로드 완료")

}


</script>


<template>


  <label
      class="
cursor-pointer
text-sm
text-gray-600
"
  >

    📄 문서 업로드

    <input
        type="file"
        hidden
        @change="upload"
    />


  </label>


</template>