import React, { useState, useEffect } from "react";
import axios from "axios";
import ChannelBox from "./SidebarChannelBox";
import SidebarClipsSetting from "./SidebarClipsSetting";
import { useSidebar } from "./ClipperContext.tsx";


function AppWithSidebar() {
  const [sortedBroadcasters, setSortedBroadcasters] = useState([]);
  const { 
    sortType, 
    isCollapsed, 
    broadcastersData, // Используем данные из контекста
  } = useSidebar();

  // Сортировка массива стримеров при изменении sortType или данных стримеров
  useEffect(() => {
    const sortBroadcasters = () => {
      let sorted = [];
      switch (sortType) {
        case "name-asc":
          sorted = [...broadcastersData].sort((a, b) =>
            a.display_name.toLowerCase().localeCompare(b.display_name.toLowerCase())
          );
          break;
        case "name-desc":
          sorted = [...broadcastersData].sort((a, b) =>
            b.display_name.toLowerCase().localeCompare(a.display_name.toLowerCase())
          );
          break;
        case "weeklyClipsCount":
          sorted = [...broadcastersData].sort((a, b) => {
            const aCount = a.stats?.weeklyClipsCount || 0;
            const bCount = b.stats?.weeklyClipsCount || 0;
            return aCount - bCount;
          });
          break;
        case "weeklyClipsCount-desc":
        default:
          sorted = [...broadcastersData].sort((a, b) => {
            const aCount = a.stats?.weeklyClipsCount || 0;
            const bCount = b.stats?.weeklyClipsCount || 0;
            return bCount - aCount;
          });
      }
      setSortedBroadcasters(sorted);
    };

    sortBroadcasters();
  }, [sortType, broadcastersData]); // Зависимость от broadcastersData вместо broadcasters

  return (
    <div className={`${isCollapsed ? "sidebar-collapsed" : "sidebar"}`}>
      <ChannelBox broadcasters={sortedBroadcasters} />
      <SidebarClipsSetting />
    </div>
  );
}

export default AppWithSidebar;
