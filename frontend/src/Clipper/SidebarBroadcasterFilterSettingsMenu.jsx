import React from "react";
import { useSidebar } from "./ClipperContext.tsx";

const BroadcasterFilterSettingsMenu = () => {
  const { setSortType } = useSidebar();
  return (
    <div className="broadcaster-filter-settings-menu">
      <div className="settings-menu-item">
        <div className="settings-menu-button" onClick={() => setSortType("name-asc")}>
          По имени <i className="bi-sort-alpha-down"></i>
        </div>
        <div className="settings-menu-button" onClick={() => setSortType("name-desc")}>
          По имени <i className="bi-sort-alpha-down-alt"></i>
        </div>
        <div className="settings-menu-button" onClick={() => setSortType("viewerCount")}>
          По зрителям <i className="bi-sort-alpha-down-alt"></i>
        </div>
        <div className="settings-menu-button" onClick={() => setSortType("weeklyClipsCount-desc")}>
          По клипам <i className="bi-sort-numeric-down-alt"></i>
        </div>
        <div className="settings-menu-button" onClick={() => setSortType("weeklyClipsCount")}>
          По клипам <i className="bi-sort-numeric-down"></i>
        </div>
      </div>
    </div>
  );
};

export default BroadcasterFilterSettingsMenu;
