import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Outlet, useNavigate } from 'react-router-dom';
import Header from './components/header/Header';
import NavBar from './components/navbar/NavBar';
import HomePage from './pages/general/home/HomePage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import VerifyPage from './pages/auth/VerifyPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ForgotPasswordVerifyPage from './pages/auth/ForgotPasswordVerifyPage';

// Main App Views
import { ProfilePage } from './pages/general/profile/ProfilePage';
import { InventoryPage } from './pages/general/storage/InventoryPage';
import './App.css';

// Component Layout riêng cho Dashboard để chứa thanh NavBar dọc cố định bên trái
function DashboardLayout({ userId, isLoggedIn, onLogout }: { userId: string; isLoggedIn: boolean; onLogout: () => void }) {
  const navigate = useNavigate();
  
  return (
    <div className="flex bg-slate-50 min-h-screen">
      {/* Cố định Sidebar bên trái */}
      <NavBar 
        userId={userId}
        isLoggedIn={isLoggedIn}
        onLogout={onLogout}
        onLogin={() => navigate('/login')}
      />
      
      {/* Khu vực hiển thị nội dung trang, cách lề trái 100px để không bị NavBar đè lên */}
      <div className="flex-1 ml-[100px]">
        <Outlet />
      </div>
    </div>
  );
}

// Giả lập các Component tạm thời cho các trang mới (Bạn thay thế bằng file thật của bạn sau)
const OfficeDashboard = () => <div className="p-8"><h1 className="text-2xl font-bold">Dashboard (/office)</h1></div>;
const ExplorePage = () => <div className="p-8"><h1 className="text-2xl font-bold">Explore Page (/explore)</h1></div>;
const AllAuctionsPage = () => <div className="p-8"><h1 className="text-2xl font-bold">All Auctions (/auctions/hub)</h1></div>;
const MyBidsPage = () => <div className="p-8"><h1 className="text-2xl font-bold">My Bids (/auction/joined)</h1></div>;
const AccountPage = () => <div className="p-8"><h1 className="text-2xl font-bold">Account Page (/account)</h1></div>;

function App() {
  // Giả định đối tượng user có thêm trường id, nếu không có bạn có thể thay thế bằng username hoặc chuỗi mặc định
  const [user, setUser] = useState<{ id?: string; username: string; initials: string } | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const savedUser = localStorage.getItem('bidvault_user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setIsLoading(false);
  }, []);

  const handleLoginSuccess = (userData: { id?: string; username: string; initials: string }) => {
    localStorage.setItem('bidvault_user', JSON.stringify(userData));
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('bidvault_user');
    setUser(null);
  };

  if (isLoading) {
    return null;
  }

  // Lấy userId động từ thông tin đăng nhập, nếu chưa đăng nhập thì để mặc định tạm thời là 'guest'
  const currentUserId = user?.id || user?.username || 'guest';

  return (
    <BrowserRouter>
      {/* Header ngang toàn cục vẫn giữ nguyên */}
      <Header
        isLoggedIn={!!user}
        user={user ?? undefined}
        onLogout={handleLogout}
      />

      <Routes>
        {/* Core Global Features */}
        <Route path="/" element={<HomePage />} />

        {/* Cụm Router dạng lồng (Nested Routes) chia sẻ chung Sidebar dọc */}
        <Route element={
          <DashboardLayout 
            userId={currentUserId} 
            isLoggedIn={!!user} 
            onLogout={handleLogout} 
          />
        }>
          <Route path="/office" element={<OfficeDashboard />} />
          <Route path="/explore" element={<ExplorePage />} />
          <Route path="/auctions/hub" element={<AllAuctionsPage />} />
          <Route path="/auction/joined" element={<MyBidsPage />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/profile/:userId" element={<ProfilePage />} />
          <Route path="/inventory" element={<InventoryPage />} />
        </Route>

        {/* Authentication Contexts */}
        <Route
          path="/login"
          element={<LoginPage onLoginSuccess={handleLoginSuccess} />}
        />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forget-password" element={<ForgotPasswordPage />} />

        {/* Token Validation Checks */}
        <Route path="/verify/user" element={<VerifyPage />} />
        <Route path="/verify/forget-password" element={<ForgotPasswordVerifyPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;