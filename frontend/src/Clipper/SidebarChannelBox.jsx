import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import HomeButton from "./SidebarHomeButton.jsx";
import BroadcastersFilter from "./SidebarBroadcastersFilter";
import ToggleButton from "./SidebarToggleButton";
import { useSidebar } from "./ClipperContext.tsx";

function Sidebar({ broadcasters = [] }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toggleCollapse, isCollapsed, broadcasterStatus } = useSidebar();
  const [updatedBroadcasters, setUpdatedBroadcasters] = useState([]);

  // Обновление списка стримеров при изменении пропсов
  useEffect(() => {
    setUpdatedBroadcasters(broadcasters);
  }, [broadcasters]);
  
  // Отдельный эффект для обработки изменений статуса
  useEffect(() => {
    if (!broadcasterStatus) return;
    
    setUpdatedBroadcasters((prev) =>
      prev.map((streamer) => ({
        ...streamer,
        isActive: String(streamer.id) === String(broadcasterStatus.broadcasterIds) 
                 ? broadcasterStatus.Status 
                 : (streamer.isActive || false)
      }))
    );
  }, [broadcasterStatus]);

  const handleChannelClick = (broadcasterId) => {
    navigate(`/app/clips/${broadcasterId}`);
  };

  useEffect(() => {
    const selectedBroadcaster = updatedBroadcasters.find(b => String(b.id) === String(id));
    document.title = selectedBroadcaster ? `Clipper | ${selectedBroadcaster.display_name}` : "Clipper";
  }, [id, updatedBroadcasters]);
  
  return (
    <div className="channel-box">
      <HomeButton />
      <ToggleButton />
      <BroadcastersFilter />
      <div className="channels-list">
        {updatedBroadcasters.length > 0 ? (
          updatedBroadcasters.map((broadcaster) => (
            <div
              key={broadcaster.id}
              className={`channel ${String(broadcaster.id) === String(id) ? "channel-selected" : ""}
                          ${broadcaster.stats?.weeklyClipsCount ? "" : "no-clips"}`}
              onClick={() => handleChannelClick(broadcaster.id)}
            >
              <button className="channel-entity">
                <div className="channel-image-container">
                  <img
                    src={broadcaster.profile_image_url}
                    alt={broadcaster.display_name}
                    width="30"
                    height="30"
                  />
                  {broadcaster.isActive && (
                    <div className="live-indicator" title={broadcaster.streamInfo ? broadcaster.streamInfo.streamTitle : 'Live Now'}></div>
                  )}
                </div>
                
                {!isCollapsed && (
                  <div className="channel-entity-info">
                    <div className="channel-entity-info-name">
                      {broadcaster.display_name}
                      {broadcaster.isActive && (
                        <div className="live-status">
                        <span className="live-tag">LIVE</span>
                        {broadcaster.isActive && broadcaster.streamInfo && (
                        <div className="stream-info" title={`Playing ${broadcaster.streamInfo.gameName}`}>
                          <span className="viewers-count">
                            {broadcaster.streamInfo.viewerCount || 0} 
                          </span>
                        </div>
                      )}
                        </div>
                      )}
                    </div>
                    <div className="channel-entity-info-stats">
                      {broadcaster.stats?.weeklyClipsCount > 0 && (
                        <div className="channel-entity-info-clips">
                          <div
                            className={
                              broadcaster.stats.weeklyClipsCount < 10
                                ? "bi-collection"
                                : "bi-collection-fill"
                            }
                          ></div>
                          {broadcaster.stats.weeklyClipsCount}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </button>
            </div>
          ))
        ) : (
          <div className="loader-box">
            <div className="loader"></div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Sidebar;
