import React, { useState, useEffect } from "react";
import { useSidebar } from "./ClipperContext.tsx";

function ClipsSetting() {
    const { toggleCollapse, isCollapsed, toggleClipsFilterSettingsMenu, isClipsFilterSettingsMenuOpen } = useSidebar();

    return (
        isCollapsed ?
            <div className="sidebar-clips-setting" onClick={(e) => {
                e.stopPropagation();
                toggleClipsFilterSettingsMenu();
            }}>
                <button className="sidebar-toggle-button-collapsed bi bi-sliders2">
                </button>
            </div>
            :
            <div className="sidebar-toggle-button">
                    Фильтры 
                    <div className="sidebar-toggle-button-item-icon" onClick={(e) => {
                        e.stopPropagation();
                        toggleClipsFilterSettingsMenu();
                    }}>
                    <button className="bi bi-sliders2">
                    </button>
                    </div>
            </div>
    );
}

export default ClipsSetting;