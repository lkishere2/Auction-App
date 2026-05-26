import { useState } from 'react';
import { Outlet } from 'react-router-dom'; // Khai báo Outlet để chuyển đổi component trang con linh hoạt
import NavBar  from '../../../components/navbar/NavBar';

export default function DashboardLayout() {
  const [isLoggedIn, setIsLoggedIn] = useState(true);
  const mockUserId = "dev-999"; // ID động của user

  return (
    <div className="flex bg-slate-100 min-h-screen">
      {/* Thanh NavBar dọc ở cạnh trái */}
      <NavBar 
        userId={mockUserId}
        isLoggedIn={isLoggedIn}
        onLogout={() => setIsLoggedIn(false)}
        onLogin={() => setIsLoggedIn(true)}
      />

      {/* Vùng hiển thị nội dung chính tự động thay đổi theo URL tuyến đường */}
      <main className="flex-1 ml-[100px] p-8">
         <Outlet />
      </main>
    </div>
  );
}