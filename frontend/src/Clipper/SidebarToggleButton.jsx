import React from "react";
import { useSidebar } from "./ClipperContext.tsx";

function ToggleButton() {
    const { toggleCollapse, isCollapsed } = useSidebar();

    return (
        isCollapsed ?
            <div className="sidebar-toggle-button-collapsed" onClick={toggleCollapse}>
                <div className="sidebar-toggle-button-item-icon-collapsed">
                    <button className="bi bi-arrow-right"></button>
                </div>
            </div>
            :
            <div className="sidebar-toggle-button">
                <div className="sidebar-toggle-button-item-text">Каналы</div>
                <div className="sidebar-toggle-button-item-icon" onClick={toggleCollapse}>
                    <button className="bi bi-arrow-left"></button>
                </div>
            </div>
    );
}

export default ToggleButton;