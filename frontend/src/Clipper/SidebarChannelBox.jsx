import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import HomeButton from "./SidebarHomeButton.jsx";
import BroadcastersFilter from "./SidebarBroadcastersFilter";
import ToggleButton from "./SidebarToggleButton";
import { useSidebar } from "./ClipperContext.tsx";

function Sidebar({ broadcasters }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const { toggleCollapse, isCollapsed, broadcasterStatus, broadcastersData } = useSidebar(); // Получаем данные из контекста

  const handleChannelClick = (broadcasterId) => {
    navigate(`/app/clips/${broadcasterId}`);
  };

  useEffect(() => {
    const selectedBroadcaster = broadcasters.find(
      (b) => String(b.broadcaster.id) === String(id)
    );
    document.title = selectedBroadcaster
      ? `Clipper | ${selectedBroadcaster.broadcaster.display_name}`
      : "Clipper";
  }, [id, broadcasters]);

  return (
    <div className="channel-box">
      <HomeButton />
      <ToggleButton />
      <BroadcastersFilter />
      <div className="channels-list">
        {broadcasters.length > 0 ? (
          broadcasters.map((broadcaster) => (
            <div
              key={broadcaster.broadcaster.id}
              className={`channel ${String(broadcaster.broadcaster.id) === String(id) ? "channel-selected" : ""}
                          ${broadcaster.clipStats?.weeklyClipsCount ? "" : "no-clips"}`}
              onClick={() => handleChannelClick(broadcaster.broadcaster.id)}
            >
              <button className="channel-entity">
                <div className="channel-image-container">
                  <img
                    src={broadcaster.broadcaster.profile_image_url}
                    alt={broadcaster.broadcaster.display_name}
                    width="30"
                    height="30"
                  />
                  {broadcaster.live && (
                    <div className="live-indicator" title={broadcaster.streamInfo ? broadcaster.streamInfo.streamTitle : 'Live Now'}></div>
                  )}
                </div>

                {!isCollapsed && (
                  <div className="channel-entity-info">
                    <div className="channel-entity-info-name">
                      {broadcaster.broadcaster.display_name}
                      {broadcaster.live && (
                        <div className="live-status">
                          <span className="live-tag">LIVE</span>
                          {broadcaster.live && broadcaster.streamInfo && (
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
                      {broadcaster.clipStats?.weeklyClipsCount > 0 && (
                        <div className="channel-entity-info-clips">
                          <div
                            className={
                              broadcaster.clipStats.weeklyClipsCount < 10
                                ? "bi-collection"
                                : "bi-collection-fill"
                            }
                          ></div>
                          {broadcaster.clipStats.weeklyClipsCount}
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