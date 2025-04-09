import React from "react";
import { useSidebar } from "./ClipperContext.tsx";

function BroadcastersFilter() {
  const { sortType, isCollapsed, toggleBroadcsterFilterSettingsMenu } = useSidebar();

  const getSortLabel = () => {
    switch (sortType) {
      case "name-asc":
        return <>По имени <i className="bi-sort-alpha-down"></i></>;
      case "name-desc":
        return <>По имени <i className="bi-sort-alpha-down-alt"></i></>;
      case "viewerCount":
        return <>По зрителям <i className="bi-sort-alpha-down-alt"></i></>;
      case "weeklyClipsCount-desc":
        return <>По клипам <i className="bi-sort-numeric-down-alt"></i></>;
      case "weeklyClipsCount":
      default:
        return <>По клипам <i className="bi-sort-numeric-down"></i></>;
    }
  };

  return isCollapsed ? (
    <div
      className="sidebar-toggle-button-collapsed"
      onClick={(e) => {
        e.stopPropagation();
        toggleBroadcsterFilterSettingsMenu();
      }}
    >
      <button className="bi bi-arrow-down-up settings-button"></button>
    </div>
  ) : (
    <div
      className="sidebar-broadcaster-filter"
      onClick={(e) => {
        e.stopPropagation();
        toggleBroadcsterFilterSettingsMenu();
      }}
    >
      <button className="sidebar-broadcaster-filter-toggle-button settings-button">
        Сортировка по:
      </button>
      <div className="sidebar-broadcaster-filter-sort-by">
        {getSortLabel()} 
      </div>
    </div>
  );
}

export default BroadcastersFilter;
