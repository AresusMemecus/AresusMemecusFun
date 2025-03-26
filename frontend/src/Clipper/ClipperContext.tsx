import React, { createContext, useContext, useState, type ReactNode, useEffect } from "react";
import axios from "axios";

interface SidebarContextType {
    fetchBroadcastersWithStreamInfo: () => Promise<any[]>;
    isOpen: boolean;
    isCollapsed: boolean;
    toggleSidebar: () => void;
    toggleCollapse: () => void;

    isOpenBroadcsterFilterSettingsMenu: boolean;
    toggleBroadcsterFilterSettingsMenu: (forceClose?: boolean) => void;

    isOpenClipsFilterSettingsMenu: boolean;
    toggleClipsFilterSettingsMenu: (forceClose?: boolean) => void;

    sortType: string;
    setSortType: (type: string) => void;
    selectedClip: any | null;
    setSelectedClip: (clip: any | null) => void;

    status: ClipsStatus;

    remainingTime: number;
    setRemainingTime: (time: number) => void;

    broadcasterStatus: {
        Status: boolean;
        broadcasterIds: string;
    };

    broadcastersData: any[];
    updateBroadcastersData: () => Promise<void>;
}

interface ClipsStatus {
    secondsRemaining: number;
    isUpdating: boolean;
}

interface StreamerStatusData {
    isActive: boolean;
    timestamp: number;
}

const SidebarContext = createContext<SidebarContextType | undefined>(undefined);

export const SidebarProvider = ({ children }: { children: ReactNode }) => {
    const [isOpen, setIsOpen] = useState(true);
    const [isCollapsed, setIsCollapsed] = useState(false);
    const [isOpenBroadcsterFilterSettingsMenu, setIsOpenBroadcsterFilterSettingsMenu] = useState(false);
    const [isOpenClipsFilterSettingsMenu, setIsOpenClipsFilterSettingsMenu] = useState(false);
    const [sortType, setSortType] = useState("default");
    const [selectedClip, setSelectedClip] = useState<any | null>(null);

    const toggleSidebar = () => setIsOpen((prev) => !prev);
    const toggleCollapse = () => setIsCollapsed((prev) => !prev);

    const toggleBroadcsterFilterSettingsMenu = (forceClose?: boolean) => {
        setIsOpenBroadcsterFilterSettingsMenu(forceClose !== undefined ? false : (prev) => !prev);
    };

    const toggleClipsFilterSettingsMenu = (forceClose?: boolean) => {
        setIsOpenClipsFilterSettingsMenu(forceClose !== undefined ? false : (prev) => !prev);
    };

    const [status, setStatus] = useState<ClipsStatus>({
        secondsRemaining: 0,
        isUpdating: false,
    });

    const [broadcasterStatus, setBroadcasterStatus] = useState({
        Status: false,
        broadcasterIds: '',
    });

    const [broadcastersData, setBroadcastersData] = useState<any[]>([]);

    const updateBroadcastersData = async () => {
        const fetchedBroadcasters = await fetchBroadcastersWithStreamInfo();
        setBroadcastersData(fetchedBroadcasters);
    };

    const BASE_URL = process.env.REACT_APP_BASE_URL;

    // Переменная для хранения закэшированных стримеров
    let broadcastersCache: {
        data: any[] | null;
        timestamp: number;
    } = {
        data: null,
        timestamp: 0
    };

    const fetchBroadcastersWithStreamInfo = async () => {
        try {
            // Проверяем наличие кэша и его свежесть (не старше 2 минут)
            const now = Date.now();
            if (broadcastersCache.data && (now - broadcastersCache.timestamp < 2 * 60 * 1000)) {
                console.log("Using cached broadcasters data");
                return broadcastersCache.data;
            }
            
            // Используем новый API эндпоинт для получения стримеров с информацией о статусе стрима
            const response = await fetchWithRetry(
                `https://${BASE_URL}/api/broadcasters/with-stream-info`, 
                { headers: { "Cache-Control": "no-cache" } }
            );
            
            // Подготавливаем массив стримеров с базовой информацией
            const broadcasters = response.data.map(item => {
                const broadcaster = item.broadcaster;
                broadcaster.isActive = item.live;
                broadcaster.streamInfo = item.streamInfo;
                return broadcaster;
            });
            
            // Ограничиваем количество одновременных запросов - обрабатываем не более 5 за раз
            const result: any[] = [];
            const batchSize = 5;
            
            for (let i = 0; i < broadcasters.length; i += batchSize) {
                const batch = broadcasters.slice(i, i + batchSize);
                // Загружаем статистику для пакета стримеров
                const batchWithStats = await Promise.all(
                    batch.map(broadcaster => fetchBroadcasterStats(broadcaster))
                );
                result.push(...batchWithStats);
            }
            
            // Обновляем кэш
            broadcastersCache = {
                data: result,
                timestamp: now
            };
            
            return result;
        } catch (error) {
            console.error("Error fetching broadcasters with stream info:", error);
            
            // В случае ошибки возвращаем старый кэш, если он есть
            if (broadcastersCache.data) {
                console.log("Using stale cache due to error");
                return broadcastersCache.data;
            }
            
            return [];
        }
    };
    
    // Утилитарная функция для выполнения запроса с повторными попытками
    const fetchWithRetry = async (url, options = {}, retries = 2, delay = 1000) => {
        try {
            return await axios.get(url, options);
        } catch (error) {
            if (retries === 0) {
                throw error;
            }
            // Экспоненциальная задержка перед повторной попыткой
            await new Promise(resolve => setTimeout(resolve, delay));
            return fetchWithRetry(url, options, retries - 1, delay * 2);
        }
    };

    // Хелпер-функция для получения статистики для стримера
    const fetchBroadcasterStats = async (broadcaster) => {
        try {
            // Проверяем, есть ли кэшированные данные
            const cachedData = localStorage.getItem(`broadcaster_stats_${broadcaster.id}`);
            
            if (cachedData) {
                try {
                    const cached = JSON.parse(cachedData);
                    const cachedTime = new Date(cached.timestamp);
                    const now = new Date();
                    
                    // Используем кэш, если он не старше 10 минут
                    if ((now.getTime() - cachedTime.getTime()) < 10 * 60 * 1000) {
                        return {
                            ...broadcaster,
                            stats: cached.data
                        };
                    }
                } catch (e) {
                    // Игнорируем ошибки парсинга кэша
                }
            }
            
            // Получаем свежую статистику с механизмом повторных попыток
            const statsResponse = await fetchWithRetry(
                `https://${BASE_URL}/api/stats/${broadcaster.id}`, 
                { headers: { "Cache-Control": "no-cache" } }
            );
            
            // Кэшируем полученные данные
            localStorage.setItem(`broadcaster_stats_${broadcaster.id}`, JSON.stringify({
                timestamp: new Date(),
                data: statsResponse.data
            }));
            
            return {
                ...broadcaster,
                stats: statsResponse.data,
            };
        } catch (statsError) {
            console.warn(`Failed to fetch stats for broadcaster ${broadcaster.id}:`, statsError);
            
            // Если есть устаревший кэш, используем его как запасной вариант
            try {
                const cachedData = localStorage.getItem(`broadcaster_stats_${broadcaster.id}`);
                if (cachedData) {
                    const cached = JSON.parse(cachedData);
                    return {
                        ...broadcaster,
                        stats: cached.data,
                        statsFromCache: true
                    };
                }
            } catch (e) {
                // Игнорируем ошибки при доступе к кэшу
            }
            
            return broadcaster;
        }
    };

    // Обработка кликов вне меню для закрытия меню "Broadcaster"
    useEffect(() => {
        const handleBroadcasterClickOutside = (event: MouseEvent) => {
            const target = event.target as HTMLElement;
            if (
                isOpenBroadcsterFilterSettingsMenu &&
                !target.closest(".broadcaster-filter-settings-menu")
            ) {
                toggleBroadcsterFilterSettingsMenu(true);
            }
        };

        document.addEventListener("click", handleBroadcasterClickOutside, true);
        return () => {
            document.removeEventListener("click", handleBroadcasterClickOutside, true);
        };
    }, [isOpenBroadcsterFilterSettingsMenu, toggleBroadcsterFilterSettingsMenu]);

    // Обработка кликов вне меню для закрытия меню "Clips"
    useEffect(() => {
        const handleClipsClickOutside = (event: MouseEvent) => {
            const target = event.target as HTMLElement;
            if (
                isOpenClipsFilterSettingsMenu &&
                !target.closest(".clips-filter-settings-menu")
            ) {
                toggleClipsFilterSettingsMenu(true);
            }
        };

        document.addEventListener("click", handleClipsClickOutside, true); // capture phase
        return () => {
            document.removeEventListener("click", handleClipsClickOutside, true);
        };
    }, [isOpenClipsFilterSettingsMenu, toggleClipsFilterSettingsMenu]);

    // Загрузка sortType из localStorage при инициализации
    useEffect(() => { 
        const storedSortType = localStorage.getItem("sortType");
        if (storedSortType) {
            setSortType(storedSortType);
        }
    }, []);

    // Сохранение sortType в localStorage при изменении
    useEffect(() => {
        localStorage.setItem("sortType", sortType);
    }, [sortType]);

    // Функция для создания надежного WebSocket соединения
    const createReliableWebSocket = (url: string, onMessage: (data: any) => void) => {
        let socket: WebSocket | null = null;
        let pingInterval: NodeJS.Timeout | null = null;
        let reconnectTimeout: NodeJS.Timeout | null = null;
        let reconnectAttempts = 0;
        const maxReconnectAttempts = 10;
        
        // Функция для очистки интервалов
        const clearTimers = () => {
            if (pingInterval) {
                clearInterval(pingInterval);
                pingInterval = null;
            }
            if (reconnectTimeout) {
                clearTimeout(reconnectTimeout);
                reconnectTimeout = null;
            }
        };

        // Функция для установки соединения
        const connect = () => {
            clearTimers();
            
            // Если уже есть сокет, закрываем его
            if (socket) {
                try {
                    socket.close();
                } catch (err) {
                    // Ошибка при закрытии сокета
                }
            }
            
            socket = new WebSocket(url);
            
            socket.onopen = () => {
                reconnectAttempts = 0;
                
                // Устанавливаем пинг каждые 20 секунд для поддержания активности соединения
                pingInterval = setInterval(() => {
                    if (socket && socket.readyState === WebSocket.OPEN) {
                        try {
                            // Отправляем пинг в формате, который ожидает сервер
                            socket.send(JSON.stringify({ type: 'ping' }));
                        } catch (err) {
                            reconnect();
                        }
                    } else {
                        reconnect();
                    }
                }, 20000);
            };
            
            socket.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    if (data.type === 'pong') {
                        return;
                    }
                    onMessage(data);
                } catch (error) {
                    // Ошибка при обработке сообщения
                }
            };
            
            socket.onerror = (error) => {
                reconnect();
            };
            
            socket.onclose = (event) => {
                clearTimers();
                reconnect();
            };
        };
        
        // Функция для переподключения с экспоненциальной задержкой
        const reconnect = () => {
            if (reconnectTimeout) return; // Уже запланировано переподключение
            
            if (reconnectAttempts < maxReconnectAttempts) {
                // Экспоненциальная задержка: 1с, 2с, 4с, 8с, ...
                const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
                
                reconnectTimeout = setTimeout(() => {
                    reconnectTimeout = null;
                    reconnectAttempts++;
                    connect();
                }, delay);
            }
        };
        
        // Запускаем подключение
        connect();
        
        // Возвращаем функцию для закрытия соединения
        return {
            close: () => {
                clearTimers();
                if (socket) {
                    try {
                        socket.close(1000, "Закрытие по запросу");
                    } catch (err) {
                        // Ошибка при закрытии WebSocket
                    }
                    socket = null;
                }
            }
        };
    };
    
    // Подключаем WebSocket соединения
    useEffect(() => {
        // Соединение для статуса бродкастера
        const broadcasterSocketConnection = createReliableWebSocket(
            `wss://${BASE_URL}/ws/clips/status/broadcaster`,
            (data) => {
                setBroadcasterStatus(data);
                
                if (data.Status === false) {
                    updateBroadcastersData();
                }
            }
        );
        
        // Соединение для таймера
        const timerSocketConnection = createReliableWebSocket(
            `wss://${BASE_URL}/ws/clips/status/timer`,
            (data) => {
                if (typeof data.secondsRemaining === 'number') {
                    setStatus(data);
                }
            }
        );
        
        // Запрос статуса клипов через HTTP для начальных данных
        const fetchClipsStatus = async () => {
            try {
                const response = await fetch(`https://${BASE_URL}/api/clips/status`);
                if (response.ok) {
                    const data = await response.json();
                    setStatus(data);
                }
            } catch (error) {
                // Ошибка при запросе статуса
            }
        };
        fetchClipsStatus();
        
        // Начальная загрузка данных
        updateBroadcastersData();
        
        // Функция очистки при размонтировании
        return () => {
            broadcasterSocketConnection.close();
            timerSocketConnection.close();
        };
    }, []);

    const [remainingTime, setRemainingTime] = useState(status.secondsRemaining);

    useEffect(() => {
      setRemainingTime(status.secondsRemaining); // Синхронизируем с сервером
    }, [status.secondsRemaining]);

    useEffect(() => {
      // Запускаем локальный таймер только если сервер не в процессе обновления
      let timer;
      if (!status.isUpdating && remainingTime > 0) {
        timer = setInterval(() => {
          setRemainingTime((prevTime) => {
            if (prevTime <= 0) {
              clearInterval(timer);
              return 0;
            }
            return prevTime - 1;
          });
        }, 1000);
      }

      return () => {
        if (timer) clearInterval(timer);
      };
    }, [remainingTime, status.isUpdating]);

    // Добавляем соединение с WebSocket для получения статуса стримов
    useEffect(() => {
        // Используем полный URL вместо относительного, чтобы избежать проблем с маршрутизацией
        const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsHost = window.location.host;
        
        // Подключаем WebSocket
        const streamStatusSocket = new WebSocket(`${wsProtocol}//${wsHost}/ws/stream/status`);
        
        streamStatusSocket.onopen = () => {
            console.log('WebSocket соединение установлено для статуса стримов');
            // Отправляем запрос на получение статусов
            streamStatusSocket.send(JSON.stringify({ action: "GET_ALL_STATUSES" }));
        };
        
        streamStatusSocket.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                console.log("Получено WebSocket сообщение:", data);
                
                if (data.Status !== undefined && data.broadcasterIds) {
                    setBroadcasterStatus(data);
                    updateBroadcasterActiveStatus(data.broadcasterIds, data.Status);
                    
                    // Сохраняем обновленный статус
                    saveStreamersStatusToStorage(data.broadcasterIds, data.Status);
                }
            } catch (error) {
                console.error('Ошибка при обработке сообщения WebSocket:', error);
            }
        };
        
        return () => {
            streamStatusSocket.close();
        };
    }, []);

    // Добавьте эту функцию в SidebarProvider для обновления статуса активности
    const updateBroadcasterActiveStatus = (broadcasterId, isActive) => {
        setBroadcastersData(prev => prev.map(broadcaster => 
            broadcaster.id === broadcasterId 
                ? { ...broadcaster, isActive } 
                : broadcaster
        ));
    };

    // При инициализации запрашиваем статусы
    useEffect(() => {
        // Запрашиваем статусы стримеров сразу при загрузке
        axios.get('/api/streamers/status')
            .then(response => {
                response.data.forEach(status => {
                    updateBroadcasterActiveStatus(status.broadcasterId, status.isLive);
                });
            })
            .catch(error => {
                console.error('Ошибка при получении статусов стримеров:', error);
            });
    }, []);

    // Добавим функцию для сохранения статусов стримеров
    const saveStreamersStatusToStorage = (broadcasterId, isActive) => {
        try {
            // Получаем текущие сохраненные статусы
            const savedData = localStorage.getItem('streamersStatus') || '{}';
            const statusData = JSON.parse(savedData);
            
            // Обновляем статус конкретного стримера
            statusData[broadcasterId] = {
                isActive,
                timestamp: Date.now() 
            };
            
            // Сохраняем обновленные данные
            localStorage.setItem('streamersStatus', JSON.stringify(statusData));
        } catch (error) {
            console.error('Ошибка при сохранении статусов стримеров:', error);
        }
    };

    // Добавляем загрузку статусов из localStorage при инициализации
    useEffect(() => {
        try {
            const savedData = localStorage.getItem('streamersStatus');
            if (savedData) {
                const statusData = JSON.parse(savedData) as Record<string, StreamerStatusData>;
                const MAX_CACHE_AGE = 60 * 60 * 1000;
                const now = Date.now();
                
                Object.entries(statusData).forEach(([broadcasterId, data]) => {
                    if (data.timestamp && now - data.timestamp < MAX_CACHE_AGE) {
                        updateBroadcasterActiveStatus(broadcasterId, data.isActive);
                    }
                });
            }
        } catch (error) {
            console.error('Ошибка при загрузке сохраненных статусов стримеров:', error);
        }
    }, []);

    return (
        <SidebarContext.Provider
            value={{
                isOpen,
                isCollapsed,
                toggleSidebar,
                toggleCollapse,
                isOpenBroadcsterFilterSettingsMenu,
                toggleBroadcsterFilterSettingsMenu,
                isOpenClipsFilterSettingsMenu,
                toggleClipsFilterSettingsMenu,
                sortType,
                setSortType,
                selectedClip,
                setSelectedClip,
                status,
                remainingTime,
                setRemainingTime,
                fetchBroadcastersWithStreamInfo,
                broadcasterStatus,
                broadcastersData,
                updateBroadcastersData,
            }}
        >
            {children}
        </SidebarContext.Provider>
    );
};

export const useSidebar = () => {
    const context = useContext(SidebarContext);
    if (!context) {
        throw new Error("useSidebar must be used within a SidebarProvider");
    }
    return context;
};

export const useSidebarBroadcsterFilterSettingsMenu = () => {
    const context = useContext(SidebarContext);
    if (!context) {
        throw new Error("useSidebarBroadcsterFilterSettingsMenu must be used within a SidebarProvider");
    }
    return context;
};

export const useClipsFilterSettingsMenu = () => {
    const context = useContext(SidebarContext);
    if (!context) {
        throw new Error("useClipsFilterSettingsMenu must be used within a SidebarProvider");
    }
    return context;
};
