/** @type {import('next').NextConfig} */
const nextConfig = {
  i18n: {
    locales: ['ko'],
    defaultLocale: 'ko',
  },

  reactStrictMode: false, // useEffect twice
  images: {
    domains: ['zippyziggytest.s3.ap-northeast-2.amazonaws.com', 'http://k.kakaocdn.net'],
    unoptimized: true,
  },
  publicRuntimeConfig: {
    // 현재 도메인 주소로 변경
    APP_URL: 'http://zippyziggy:3000',
  },

  async rewrites() {
    return [
      {
        source: '/file',
        destination: `/api/file`,
      },
    ];
  },

  async headers() {
    return [
      {
        // 모든 도메인에서의 요청을 허용하는 CORS 설정 추가
        source: '/api/:path*',
        headers: [
          {
            key: 'Access-Control-Allow-Origin',
            value: '*',
          },
          {
            key: 'Access-Control-Allow-Methods',
            value: 'GET,OPTIONS,PATCH,DELETE,POST,PUT',
          },
          {
            key: 'Access-Control-Allow-Headers',
            value:
              'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version',
          },
        ],
      },
    ];
  },
};

module.exports = nextConfig;
