import React, { createContext, useContext, useState, type ReactNode, useEffect } from "react";
import axios from "axios";

interface SidebarContextType {
    fetchBroadcasters: () => Promise<any[]>;
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

    broadcastersData: any[];
    updateBroadcastersData: () => Promise<void>;
}

interface ClipsStatus {
    secondsRemaining: number;
    isUpdating: boolean;
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

    const [broadcastersData, setBroadcastersData] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [lastFetchTime, setLastFetchTime] = useState<number | null>(null);



    const fetchBroadcasters = async (): Promise<any[]> => {
        try {
          const response = await axios.get("https://aresusmemecus.fun/api/broadcasters");
          return response.data;
        } 
        catch (error) {
          console.error("Ошибка при получении данных стримеров:", error);
          return [];
        }
      };
    
    const fetchTimer = async (): Promise<any[]> => {
        try{
            const response = await axios.get("https://aresusmemecus.fun/api/clips/timer")
            return response.data;
        }
        catch (error){
            console.error("Ошибка при получении таймера времени:", error);
            return [];
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

    const [remainingTime, setRemainingTime] = useState(status.secondsRemaining);

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


    const updateBroadcastersData = async () => {
        const currentTime = Date.now();
        const cacheExpired = lastFetchTime === null || currentTime - lastFetchTime > 2 * 60 * 1000; // Проверяем, прошло ли больше 2 минут
    
        if (cacheExpired && !loading) {
            setLoading(true);
            const fetchedBroadcasters = await fetchBroadcasters();
            setBroadcastersData(fetchedBroadcasters);
            setLastFetchTime(currentTime); // Обновляем время последнего запроса
            setLoading(false);
        }
    };

    useEffect(() => {
        // При инициализации запрашиваем данные о стримерах
        updateBroadcastersData();
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
                fetchBroadcasters,
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
