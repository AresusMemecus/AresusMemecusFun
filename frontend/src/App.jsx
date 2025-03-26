import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { useEffect } from "react";
import Sidebar from "./Clipper/Sidebar";
import Clips from "./Clipper/ClipsContent.jsx";
import { SidebarProvider, useSidebar } from "./Clipper/ClipperContext.tsx";
import Header from "./Header/Header.jsx";
import Main from "./Main/Main.jsx";
import Apps from "./App's/App's.jsx";
import ClipperMain from "./Clipper/ClipperMain.jsx";
import 'bootstrap-icons/font/bootstrap-icons.css';
import "./App.css";

// Компонент-обертка для инициализации данных
function AppContent() {
  const { updateBroadcastersData } = useSidebar();
  
  // Загрузка данных при первой инициализации
  useEffect(() => {
    // Загружаем данные о стримерах сразу после монтирования
    updateBroadcastersData();
    
    // Устанавливаем интервал для периодического обновления данных (каждые 2 минуты)
    const interval = setInterval(() => {
      updateBroadcastersData();
    }, 2 * 60 * 1000);
    
    // Очистка интервала при размонтировании компонента
    return () => clearInterval(interval);
  }, [updateBroadcastersData]);
  
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={<Main />} />
        <Route path="/app" element={<Apps />} />
        <Route 
          path="/app/clips"
          element={
            <>
              <Sidebar />
              <Clips>
              </Clips>
            </>
          }
        />
        <Route
          path="/app/clips/:id"
          element={
            <>
              <Sidebar />
              <Clips />
            </>
          }
        />
      </Routes>
    </Router>
  );
}

function App() {
  return (
    <SidebarProvider>
      <AppContent />
    </SidebarProvider>
  );
}

export default App;
