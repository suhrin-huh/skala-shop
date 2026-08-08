import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    // 개발 서버에서 /api 와 /images 요청을 로컬 백엔드로 넘긴다.
    // 이렇게 하면 프론트 코드가 배포 환경(Nginx 동일 오리진)과 똑같이 상대 경로만 호출하면 된다.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 업로드된 이미지는 dev 에서 백엔드의 /images/** 정적 매핑으로 서빙된다.
      // 이 프록시가 없으면 업로드한 대표 이미지가 화면에서 404 로 깨진다.
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  }
})
