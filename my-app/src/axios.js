import axios from 'axios';

// Attach JWT token to all requests if present
axios.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token && config.url && !config.url.includes('/api/auth/')) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

export default axios;
