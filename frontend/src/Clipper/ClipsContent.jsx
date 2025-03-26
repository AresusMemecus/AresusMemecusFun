import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import axios from "axios";
import { useSidebar } from "./ClipperContext.tsx";
import { useSidebarBroadcsterFilterSettingsMenu } from "./ClipperContext.tsx";
import { useClipsFilterSettingsMenu } from "./ClipperContext.tsx";
import BroadcasterFilterSettingsMenu from "./SidebarBroadcasterFilterSettingsMenu";
import ClipsFilterSettingsMenu from "./SidebarClipsFilterSettingsMenu";
import ClipPlayer from "./ClipPlayer.jsx";
import ClipperMain from "./ClipperMain.jsx";

const getDefaultStartDate = () => {
  const currentDate = new Date();
  const currentDay = currentDate.getDay();
  const diffToMonday = currentDay === 0 ? -6 : 1 - currentDay;
  const monday = new Date(currentDate);
  monday.setDate(currentDate.getDate() + diffToMonday);
  return monday.toISOString().split("T")[0];
};

const getDefaultEndDate = () => {
  const currentDate = new Date();
  const currentDay = currentDate.getDay();
  const diffToSunday = currentDay === 0 ? 0 : 7 - currentDay;
  const sunday = new Date(currentDate);
  sunday.setDate(currentDate.getDate() + diffToSunday);
  return sunday.toISOString().split("T")[0];
};

function Clips() {
  const { id } = useParams();
  const [clips, setClips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [noClipsFound, setNoClipsFound] = useState(false);
  const [isOpenClipPlayer, setIsOpenClipPlayer] = useState(false);

  // Инициализация дат с проверкой localStorage
  const [startDate, setStartDate] = useState(() => {
    return localStorage.getItem("startDate") || getDefaultStartDate();
  });
  const [endDate, setEndDate] = useState(() => {
    return localStorage.getItem("endDate") || getDefaultEndDate();
  });
  
  const [sortCriteria, setSortCriteria] = useState([{ type: "CREATION_DATE", order: "desc" }]);
  const { isOpenBroadcsterFilterSettingsMenu } = useSidebarBroadcsterFilterSettingsMenu();
  const { isOpenClipsFilterSettingsMenu, setIsOpenClipsFilterSettingsMenu } = useClipsFilterSettingsMenu();
  const { setSortType, setSelectedClip, selectedClip, fetchBroadcastersWithStats } = useSidebar();

  // При смене стримера сбрасываем выбранный клип и закрываем плеер
  useEffect(() => {
    setIsOpenClipPlayer(false);
    setSelectedClip(null);
  }, [id]);

  // Скроллим к выбранному клипу, когда он выбран
  useEffect(() => {
    if (selectedClip) {
      const selectedElement = document.querySelector(`.clip-entity[data-clip-id="${selectedClip.id}"]`);
      if (selectedElement) {
        selectedElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }
  }, [selectedClip]);

  const formatDateForServer = (date) => {
    if (!date || !(date instanceof Date) || isNaN(date.getTime())) {
      return null;
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };

  // Сохранение дат в localStorage при их изменении
  useEffect(() => {
    localStorage.setItem("startDate", startDate);
  }, [startDate]);

  useEffect(() => {
    localStorage.setItem("endDate", endDate);
  }, [endDate]);

  const BASE_URL = process.env.REACT_APP_BASE_URL;

  // Функция для загрузки клипов с проверкой актуальности запроса
  const fetchClips = (signal, start, end, sort) => {
    // Если параметры не переданы, используем значения из состояния
    const finalStart = start !== undefined ? start : startDate;
    const finalEnd = end !== undefined ? end : endDate;
    const finalSort = sort !== undefined ? sort : sortCriteria;
  
    setLoading(true);
    setClips([]);
    setNoClipsFound(false);
  
    let isCurrent = true; // Флаг актуальности запроса
  
    axios
      .get(`https://${BASE_URL}/api/clips/${id}`, {
        signal,
        params: {
          startDate: formatDateForServer(new Date(finalStart)),
          endDate: formatDateForServer(new Date(finalEnd)),
          sort: finalSort.map(crit => `${crit.type}:${crit.order}`).join(',')
        },
      })
      .then((response) => {
        if (isCurrent) {
          setClips(response.data);
          setLoading(false);
          setIsOpenClipPlayer(false);
          if (response.data.length === 0) {
            setNoClipsFound(true);
          }
        }
      })
      .catch((error) => {
        if (error.name === "AbortError") return;
        if (isCurrent) {
          setLoading(false);
        }
      });
  
    return () => {
      isCurrent = false;
    };
  };
  

  useEffect(() => {
    const controller = new AbortController();
    const cleanupFetch = fetchClips(controller.signal);

    return () => {
      cleanupFetch();
      controller.abort();
    };
  }, [id, startDate, endDate, sortCriteria]);

  const handleClipClick = (clip) => {
    setSelectedClip(clip);
    setIsOpenClipPlayer(true);
  };

  const handlePrevClip = () => {
    const currentIndex = clips.findIndex((clip) => clip.id === selectedClip?.id);
    if (currentIndex > 0) {
      setSelectedClip(clips[currentIndex - 1]);
    }
  };

  const handleNextClip = () => {
    const currentIndex = clips.findIndex((clip) => clip.id === selectedClip?.id);
    if (currentIndex < clips.length - 1) {
      setSelectedClip(clips[currentIndex + 1]);
    }
  };

  const calculateTotalDuration = () => {
    const totalSeconds = clips.reduce((total, clip) => total + clip.duration, 0);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    return `${hours > 0 ? hours + 'ч ' : ''}${minutes}м`;
  };

  const handleFilterReset = () => {
    setStartDate(getDefaultStartDate());
    setEndDate(getDefaultEndDate());
    setSortCriteria([{ type: "CREATION_DATE", order: "desc" }]);
    const controller = new AbortController();
    const cleanupFetch = fetchClips(controller.signal);
    return () => {
      cleanupFetch();
      controller.abort();
    };
  };

  const handleFilterApply = (newStartDate, newEndDate, newSortCriteria) => {
    const controller = new AbortController();
    // Сохраняем возвращаемую функцию очистки в переменной cleanupFetch
    const cleanupFetch = fetchClips(controller.signal, newStartDate, newEndDate, newSortCriteria);
    
    return () => {
      cleanupFetch();
      controller.abort();
    };
  };

  const handleLike = () => {
    const likedClips = JSON.parse(localStorage.getItem('likedClips')) || [];
    const currentClipId = selectedClip?.id;

    if (!currentClipId) return;

    const isLiked = likedClips.some(clip => clip.id === currentClipId);

    if (!isLiked) {
      const clipData = {
        id: currentClipId,
        broadcasterId: selectedClip.broadcasterId,
        createdAt: selectedClip.createdAt,
        title: selectedClip.title,
        creatorName: selectedClip.creatorName,
      };
      likedClips.push(clipData);
      localStorage.setItem('likedClips', JSON.stringify(likedClips));
    }
  };

  if (!id) {
    return (
      <div className="page-manager">
        <ClipperMain />
        {isOpenBroadcsterFilterSettingsMenu && <BroadcasterFilterSettingsMenu setSortType={setSortType} />}
        {isOpenClipsFilterSettingsMenu && (
          <ClipsFilterSettingsMenu
            startDate={startDate}
            setStartDate={setStartDate}
            endDate={endDate}
            setEndDate={setEndDate}
            sortCriteria={sortCriteria}
            setSortCriteria={setSortCriteria}
            handleFilterApply={handleFilterApply}
            handleFilterReset={handleFilterReset}
            closeMenu={() => setIsOpenClipsFilterSettingsMenu(false)} // Вот здесь
          />
        )}
      </div>
    );
  }

  function formatDuration(seconds) {
    const mins = Math.floor(seconds / 60); // Получаем полные минуты
    const secs = Math.floor(seconds % 60); // Получаем секунды
    const formattedSecs = secs < 10 ? `0${secs}` : secs; // Добавляем ведущий ноль, если нужно
  
    return `${mins}:${formattedSecs}`;
  }

  function formatViewsCount(count) {
    if (count % 10 === 1 && count % 100 !== 11) {
      return `${count} просмотр`;
    } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
      return `${count} просмотра`;
    } else {
      return `${count} просмотров`;
    }
  }

  function formatTimeAgo(date) {
    const now = new Date();
    const diffInSeconds = Math.floor((now - new Date(date)) / 1000);
  
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    const diffInHours = Math.floor(diffInMinutes / 60);
    const diffInDays = Math.floor(diffInHours / 24);
    const diffInMonths = Math.floor(diffInDays / 30); // Среднее количество дней в месяце
    const diffInYears = Math.floor(diffInDays / 365); // Среднее количество дней в году
  
    if (diffInSeconds < 60) {
      return "Сегодня";
    } else if (diffInMinutes < 60) {
      return `${diffInMinutes} мин. назад`;
    } else if (diffInHours < 24) {
      return `${diffInHours} ч. назад`;
    } else if (diffInDays === 1) {
      return "Вчера";
    } else if (diffInDays === 2) {
      return "Позавчера";
    } else if (diffInDays < 7) {
      return `${diffInDays} дн. назад`;
    } else if (diffInDays < 30) {
      return `${Math.floor(diffInDays / 7)} нед. назад`;
    } else if (diffInMonths < 12) {
      return `${diffInMonths} мес. назад`;
    } else {
      return `${diffInYears} г. назад`;
    }
  }

  return (
    <div className="page-manager">
      {loading ? (
        <div className="loader-box">
          <div className="loader"></div>
        </div>
      ) : (
        <>
          <div className="clips-grid">
            {clips.length > 0 ? (
              clips.map((clip) => (
                <div
                  className={`clip-entity ${selectedClip?.id === clip.id ? "active" : ""}`}
                  key={clip.id}
                  data-clip-id={clip.id}
                  onClick={() => handleClipClick(clip)}
                >
                  <div className="clip-entity-info">
                  <div className="clip-entity-info-top">
                    <div className="clip-entity-info-top-duration">{formatDuration(clip.duration)}</div>
                  </div>
                  <div className="clip-entity-info-bot">
                    <div className="clip-entity-info-bot-views">{formatViewsCount(clip.viewCount)}</div>
                    <div className="clip-entity-info-bot-created">{formatTimeAgo(clip.createdAt)}</div>
                  </div>
                  <img src={clip.thumbnailUrl} alt={clip.title} />
                  </div>
                  
                  <p>{clip.title}</p>
                </div>
              ))
            ) : (
              <div className="not-found">
                <img src="https://i.imgur.com/OePiL4a.png" alt="Not found" />
                <p>Клипов не найдено</p>
              </div>
            )}
          </div>
        </>
      )}

      {isOpenClipPlayer && (
        <ClipPlayer
          clips={clips}
          selectedClip={selectedClip}
          setIsOpenClipPlayer={setIsOpenClipPlayer}
          handlePrevClip={handlePrevClip}
          handleNextClip={handleNextClip}
          calculateTotalDuration={calculateTotalDuration}
          handleLike={handleLike}
        />
      )}

      {isOpenBroadcsterFilterSettingsMenu && <BroadcasterFilterSettingsMenu setSortType={setSortType} />}
      {isOpenClipsFilterSettingsMenu && (
        <ClipsFilterSettingsMenu
          startDate={startDate}
          setStartDate={setStartDate}
          endDate={endDate}
          setEndDate={setEndDate}
          sortCriteria={sortCriteria}
          setSortCriteria={setSortCriteria}
          handleFilterApply={handleFilterApply}
          handleFilterReset={handleFilterReset}
        />
      )}
    </div>
  );
}

export default Clips;
