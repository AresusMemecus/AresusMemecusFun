import React, { useState, useRef } from "react";

function ClipPlayer({
    clips,
    selectedClip,
    setIsOpenClipPlayer,
    handlePrevClip,
    handleNextClip,
    calculateTotalDuration,
    handleLike,
}) {
    const [likedClips, setlikedClips] = useState(() => {
        return JSON.parse(localStorage.getItem("likedClips")) || [];
    });

    const [height, setHeight] = useState(500);
    const isResizingRef = useRef(false);
    const lastHeightRef = useRef(height);
    const frameRef = useRef(null);
    
    const MIN_HEIGHT = 500;
    const MAX_HEIGHT = 700;

    const BASE_URL = process.env.REACT_APP_BASE_URL;
    const isLiked = likedClips.some(clip => clip.id === selectedClip?.id);

    const handleLikeClick = () => {
        const currentClipId = selectedClip?.id;
        if (!currentClipId) return;

        const clipData = {
            id: currentClipId,
            broadcasterName: selectedClip.broadcasterName,
            createdAt: selectedClip.createdAt,
            title: selectedClip.title,
            creatorName: selectedClip.creatorName,
        };

        const updatedLikedClips = isLiked
            ? likedClips.filter((clip) => clip.id !== currentClipId)
            : [...likedClips, clipData];

        setlikedClips(updatedLikedClips);
        localStorage.setItem("likedClips", JSON.stringify(updatedLikedClips));
    };

    const handleCopyClick = () => {
        const textToCopy = clips.find((clip) => clip.id === selectedClip?.id)?.url;
        navigator.clipboard.writeText(textToCopy);
    };

    if (!selectedClip) return null;

    const handleResize = (event) => {
        if (!isResizingRef.current) return;
        
        const resizeHandle = document.querySelector(".resize-handle");
        if (!resizeHandle) return;
        
        const container = resizeHandle.parentElement;
        const rect = container.getBoundingClientRect();
        
        // Вычисляем новую высоту
        let newHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, event.clientY - rect.top));
        
        // Проверяем, изменилась ли высота существенно (более чем на 5px)
        if (Math.abs(newHeight - lastHeightRef.current) > 5) {
            // Отменяем предыдущий запрос анимации, если он есть
            if (frameRef.current) {
                cancelAnimationFrame(frameRef.current);
            }
            
            // Планируем обновление на следующий кадр
            frameRef.current = requestAnimationFrame(() => {
                setHeight(newHeight);
                lastHeightRef.current = newHeight;
            });
        }
    };

    const startResize = (event) => {
        isResizingRef.current = true;
        document.body.style.cursor = 'ns-resize';
        document.body.style.userSelect = 'none';
        
        // Создаем прозрачный оверлей, который будет перехватывать все события мыши
        const overlay = document.createElement('div');
        overlay.id = 'resize-overlay';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.width = '100%';
        overlay.style.height = '100%';
        overlay.style.zIndex = '10000';
        overlay.style.cursor = 'ns-resize';
        document.body.appendChild(overlay);
        
        document.addEventListener("mousemove", handleResize, { passive: true });
        document.addEventListener("mouseup", stopResize);
        event.preventDefault();
    };

    const stopResize = () => {
        isResizingRef.current = false;
        document.body.style.cursor = '';
        document.body.style.userSelect = '';
        
        // Удаляем оверлей
        const overlay = document.getElementById('resize-overlay');
        if (overlay) {
            document.body.removeChild(overlay);
        }
        
        document.removeEventListener("mousemove", handleResize);
        document.removeEventListener("mouseup", stopResize);
        if (frameRef.current) {
            cancelAnimationFrame(frameRef.current);
            frameRef.current = null;
        }
    };

    return (
        <div className="clip-player">
            <div className="clip-player-page">
            <div className="clip-player-content">
                <div className="clip-player-iframe" style={{ height: height + "px" }}>
                    <iframe
                        src={`https://clips.twitch.tv/embed?clip=${selectedClip.id}&parent=${BASE_URL}&autoplay=true`}
                        allowFullScreen
                        title="Clip Player"
                        style={{ width: "100%", height: "100%" }}
                    ></iframe>
                </div>

                {/* Полоска для изменения размера */}
                <div className="resize-handle" onMouseDown={startResize}></div>

                <div className="bi bi-x-lg close-button" onClick={() => setIsOpenClipPlayer(false)}></div>
                <div
                    className={`nav-button prev-button bi bi-chevron-left ${
                        clips.findIndex((clip) => clip.id === selectedClip?.id) === 0 ? "disabled" : ""
                    }`}
                    onClick={handlePrevClip}></div>
                <div
                    className={`nav-button next-button bi bi-chevron-right ${
                        clips.findIndex((clip) => clip.id === selectedClip?.id) === clips.length - 1 ? "disabled" : ""
                    }`}
                    onClick={handleNextClip}></div>
            </div>

            <div className="clips-info">
                <div className="clips-info-top">
                    <div className="clip-info-link">
                        <div className="info-button" onClick={handleCopyClick}>
                            <i className="bi bi-link-45deg"></i>
                        </div>
                        <div>{selectedClip?.url}</div>
                    </div>
                    <div className="clip-info-items">
                        <div className="clip-info-item-number">
                            <div className="bi bi-collection-play"></div>
                            {clips.findIndex((clip) => clip.id === selectedClip?.id) + 1} / {clips.length}
                        </div>
                        |
                        <div className="clip-info-item-duration">
                            <div className="bi bi-clock"></div>
                            {calculateTotalDuration()}
                        </div>
                    </div>
                </div>
                <div className="clip-info-top-second">
                    <div className={`info-button ${isLiked ? "liked" : ""}`} onClick={handleLikeClick}>
                        {isLiked ? <div className="bi bi-heart-fill"></div> : <div className="bi bi-heart"></div>}
                    </div>
                    <div className="clip-info-author">
                        Автор клипа: {selectedClip?.creatorName}
                    </div>
                </div>
            </div>
        </div>
        </div>
    );
}

export default ClipPlayer;
