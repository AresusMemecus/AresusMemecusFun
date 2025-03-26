import React from "react";
import { useSidebar } from "./ClipperContext.tsx";
import { useNavigate } from "react-router-dom";


function HomeButton() {
    const { toggleCollapse, isCollapsed } = useSidebar();
    const navigate = useNavigate();

    return (
        isCollapsed ?
            <div className="sidebar-toggle-button-collapsed" onClick={()=> navigate("/app/clips")}>
                <div className="sidebar-toggle-button-item-icon-collapsed">
                    <button className="bi bi-house"></button>
                </div>
            </div>
            :
            <div className="sidebar-toggle-button" onClick={()=> navigate("/app/clips")}>
                <div className="sidebar-toggle-button-item-text">Главная</div>
                <div className="sidebar-toggle-button-item-icon">
                    <button className="bi bi-house"></button>
                </div>
            </div>
    );
}

export default HomeButton;