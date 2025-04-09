import React, { useEffect, useState } from "react";
import axios from "axios";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { useSidebar } from "./ClipperContext.tsx";

const ClipsFilterSettingsMenu = ({
  startDate,
  setStartDate,
  endDate,
  setEndDate,
  sortCriteria,
  setSortCriteria,
  handleFilterApply,
  handleFilterReset,
  handleRequestNewClips,
}) => {
  const { isCollapsed } = useSidebar();
  const { toggleClipsFilterSettingsMenu, setSelectedClip, status, remainingTime } = useSidebar();

  // Инициализируем диапазон дат либо из пропсов, либо по умолчанию (эта неделя)
  const getThisWeekRange = () => {
    const currentDate = new Date();
    const day = currentDate.getDay() || 7;
    const monday = new Date(currentDate);
    monday.setDate(currentDate.getDate() - day + 1);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return [monday, sunday];
  };

  const initialRange = startDate && endDate
    ? [new Date(startDate), new Date(endDate)]
    : getThisWeekRange();

  const [selectedDateRange, setSelectedDateRange] = useState(initialRange);
  const [tempStartDate, setTempStartDate] = useState(initialRange[0]);
  const [tempEndDate, setTempEndDate] = useState(initialRange[1]);
  const [tempSortCriteria, setTempSortCriteria] = useState(sortCriteria);
  const [timeLeft, setTimeLeft] = useState(0);

  // Если внешний startDate/endDate изменяются, обновляем наш диапазон
  useEffect(() => {
    if (startDate && endDate) {
      const newRange = [new Date(startDate), new Date(endDate)];
      setSelectedDateRange(newRange);
      setTempStartDate(newRange[0]);
      setTempEndDate(newRange[1]);
    }
  }, [startDate, endDate]);

  const setLastWeek = () => {
    const currentDate = new Date();
    const day = currentDate.getDay() || 7;
    const monday = new Date(currentDate);
    monday.setDate(currentDate.getDate() - day - 6); // Понедельник прошлой недели
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6); // Воскресенье прошлой недели

    setSelectedDateRange([monday, sunday]);
    setTempStartDate(monday);
    setTempEndDate(sunday);
  };

  const setThisWeek = () => {
    const [monday, sunday] = getThisWeekRange();
    setSelectedDateRange([monday, sunday]);
    setTempStartDate(monday);
    setTempEndDate(sunday);
  };

  const setToday = () => {
    const today = new Date();
    setSelectedDateRange([today, today]);
    setTempStartDate(today);
    setTempEndDate(today);
  };

  const setYesterday = () => {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    setSelectedDateRange([yesterday, yesterday]);
    setTempStartDate(yesterday);
    setTempEndDate(yesterday);
  };

  const setLastNDays = (n) => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - n);
    setSelectedDateRange([start, end]);
    setTempStartDate(start);
    setTempEndDate(end);
  };

  const handleDateChange = (dates) => {
    const [start, end] = dates;
    setSelectedDateRange([start, end]);
    setTempStartDate(start);
    setTempEndDate(end);
  };

  const formatDateForServer = (date) => {
    if (!date || !(date instanceof Date) || isNaN(date.getTime())) return null;
    return date.toISOString().split("T")[0];
  };

  const handleSortChange = (type, order) => {
    const updatedCriteria = [{ type, order }];
    setTempSortCriteria(updatedCriteria);
  };

  const handleApply = () => {
    setSelectedClip(null);
    setStartDate(tempStartDate);
    setEndDate(tempEndDate);
    setSortCriteria(tempSortCriteria);
    toggleClipsFilterSettingsMenu(true);
  };

  const formatTime = (totalSeconds) => {
    // Проверка на корректные входные данные
    if (typeof totalSeconds !== 'number' || isNaN(totalSeconds) || totalSeconds < 0) {
      return '00:00';
    }
    
    const mins = Math.floor(totalSeconds / 60);
    const secs = Math.floor(totalSeconds % 60); // Убедимся, что секунды - целое число
    const formattedTime = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    return formattedTime;
  };

  return (
    <div className={isCollapsed ? "clips-filter-settings-collapsed" : "clips-filter-settings"}>
      <div className="clips-filter-settings-menu">
        <div className="clips-filter-settings-item-header">
          <div className="clips-filter-settings-item-header-shadow"></div>
          <div className="clips-filter-settings-item-header-buttons">
            <button onClick={setLastWeek} style={{ marginLeft: '25px' }}> <i className="bi-circle-fill"></i>Предыдущая неделя</button>
            <button onClick={setThisWeek}> <i className="bi-circle-fill"></i>Эта неделя</button>
            <button onClick={setToday}><i className="bi-circle-fill"></i>Сегодня</button>
            <button onClick={setYesterday}><i className="bi-circle-fill"></i>Вчера</button>
            <button onClick={() => setLastNDays(7)}><i className="bi-circle-fill"></i>Последние 7 дней</button>
            <button onClick={() => setLastNDays(14)}><i className="bi-circle-fill"></i>Последние 14 дней</button>
            <button onClick={() => setLastNDays(30)} style={{ marginRight: '25px' }}><i className="bi-circle-fill"></i>Последние 30 дней</button>
          </div>
        </div>

        <div className="clips-filter-settings-item-content">
          <DatePicker
            onChange={handleDateChange}
            startDate={selectedDateRange[0]}
            endDate={selectedDateRange[1]}
            selectsRange
            inline
          />

          <div className="clips-filter-settings-item">
            <div className="clips-filter-settings-item-top">Сортировка клипов по:</div>
            <div className="clips-filter-settings-choose">
              <label className="checkbox-wrapper-2">
                Сначала старые
                <input
                  className="sc-gJwTLC ikxBAC"
                  type="checkbox"
                  checked={tempSortCriteria[0]?.type === "CREATION_DATE" && tempSortCriteria[0]?.order === "asc"}
                  onChange={() => handleSortChange("CREATION_DATE", "asc")}
                />
              </label>
              <label className="checkbox-wrapper-2">
                Сначала новые
                <input
                  className="sc-gJwTLC ikxBAC"
                  type="checkbox"
                  checked={tempSortCriteria[0]?.type === "CREATION_DATE" && tempSortCriteria[0]?.order === "desc"}
                  onChange={() => handleSortChange("CREATION_DATE", "desc")}
                />
              </label>
              <label className="checkbox-wrapper-2">
                Кол-ву просмотров
                <input
                  className="sc-gJwTLC ikxBAC"
                  type="checkbox"
                  checked={tempSortCriteria[0]?.type === "VIEWS" && tempSortCriteria[0]?.order === "desc"}
                  onChange={() => handleSortChange("VIEWS", "desc")}
                />
              </label>
            </div>

            <div className="clips-filter-settings-timer">
              Таймер: 
              {status.isUpdating ?
                <div className="clips-filter-settings-timer-item">
                  Обновляется
                </div>
                :
                <div className="clips-filter-settings-timer-item">
                  <div className="clips-filter-settings-timer-item-text">Обновится через:</div>
                  <div className="clips-filter-settings-timer-item-time">
                    {remainingTime > 0 ? formatTime(remainingTime) : '00:00'}
                  </div>
                </div>
              }
            </div>
          </div>
        </div>

        <div className="clips-filter-settings-item">
          <button className="primary-button" onClick={handleApply}>
            Применить
          </button>
          <button className="secondary-button" onClick={handleFilterReset}>
            Сбросить
          </button>
        </div>

      </div>
      <div className="clips-filter-settings-bg"></div>
    </div>
  );
};

export default ClipsFilterSettingsMenu;
