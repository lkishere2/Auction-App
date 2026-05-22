import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePage from './pages/general/home/HomePage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyPage from './pages/auth/VerifyPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ForgotPasswordVerifyPage from './pages/auth/ForgotPasswordVerifyPage';
import { AuthProvider } from './context/AuthContext';
import './App.css';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* General Routes */}
          <Route path="/" element={<HomePage />} />
          <Route path="/home" element={<HomePage />} />

          {/* Authentication Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forget-password" element={<ForgotPasswordPage />} />

          {/* Verification Routes */}
          <Route path="/verify/user" element={<VerifyPage />} />
          <Route path="/verify/forget-password" element={<ForgotPasswordVerifyPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;