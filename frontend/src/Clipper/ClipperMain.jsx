import React, { useState, useEffect } from "react";

function ClipperMain() {
    const [groupedClips, setGroupedClips] = useState([]);

    const handleDeleteClip = (clipId) => {
        const storedClips = localStorage.getItem("likedClips");
        if (storedClips) {
            try {
                const parsedClips = JSON.parse(storedClips);
                const updatedClips = parsedClips.filter(clip => clip.id !== clipId);
                localStorage.setItem("likedClips", JSON.stringify(updatedClips));
                
                // Обновляем состояние
                const updatedGroupedClips = {};
                updatedClips.forEach(clip => {
                    if (!updatedGroupedClips[clip.broadcasterName]) {
                        updatedGroupedClips[clip.broadcasterName] = [];
                    }
                    updatedGroupedClips[clip.broadcasterName].push(clip);
                });
                setGroupedClips(updatedGroupedClips);
            } catch (error) {
                // Удалено сообщение об ошибке
            }
        }
    };

    const handleCopyClip = (clipId) => {
        const storedClips = localStorage.getItem("likedClips");
        if (storedClips) {
            try {
                const parsedClips = JSON.parse(storedClips);
                const clipToCopy = parsedClips.find(clip => clip.id === clipId);
                
                if (clipToCopy) {
                    const textToCopy = `${clipToCopy.title} | Автор клипа: ${clipToCopy.creatorName}`;
                    navigator.clipboard.writeText(textToCopy)
                        .catch(err => {
                            // Удалено сообщение об ошибке
                        });
                }
            } catch (error) {
                // Удалено сообщение об ошибке
            }
        }
    };

    useEffect(() => {
        const storedClips = localStorage.getItem("likedClips");
        if (storedClips) {
            try {
                const parsedClips = JSON.parse(storedClips);

                // Сортируем клипы по broadcasterName
                parsedClips.sort((a, b) => a.broadcasterName.localeCompare(b.broadcasterName));

                // Группируем клипы по broadcasterName
                const grouped = parsedClips.reduce((acc, clip) => {
                    if (!acc[clip.broadcasterName]) {
                        acc[clip.broadcasterName] = [];
                    }
                    acc[clip.broadcasterName].push(clip);
                    return acc;
                }, {});

                setGroupedClips(grouped);
            } catch (error) {
                // Удалено сообщение об ошибке
            }
        }
    }, []);

    return (
            <div className="clipper-table-container">
                {Object.keys(groupedClips).length ? (
                    Object.entries(groupedClips).map(([broadcasterName, clips]) => (
                        <div key={broadcasterName} className="broadcaster-section">
                            <div className="broadcaster-name-logo">
                                <h3 className="broadcaster-name">{broadcasterName}</h3>
                                </div>
                            <table className="clips-table">
                                <thead>
                                    <tr>
                                        <th>Название</th>
                                        <th>Автор клипа</th>
                                        <th>Действия</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {clips.map((clip) => (
                                        <tr key={clip.id}>
                                            <td className="clip-title">{clip.title}</td>
                                            <td className="clip-author">{clip.creatorName}</td>
                                            <td className="clip-actions">
                                                <a href={`https://clips.twitch.tv/${clip.id}`} 
                                                   target="_blank" 
                                                   rel="noopener noreferrer"
                                                   className="clip-button">
                                                    Открыть
                                                </a>
                                                <button 
                                                    onClick={() => handleDeleteClip(clip.id)}
                                                    className="clip-button delete">
                                                    Удалить
                                                </button>
                                                <button
                                                    onClick={() => handleCopyClip(clip.id)}
                                                    className="clip-button copy">
                                                    Копировать
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    ))
                ) : (
                    <div className="no-clips-message">
                        <p>Нет сохраненных клипов</p>
                        <p className="no-clips-hint">Добавьте клипы через основной интерфейс</p>
                    </div>
                )}
            </div>
    );
}

export default ClipperMain;
