/* Native Win32 settings GUI: one checkbox + slider (trackbar) per criterion, mirroring the
 * options that used to live in the Minecraft mod's own settings screen. Saves to the same
 * config file the CLI search mode reads. No external GUI toolkit - just Win32 common controls,
 * so the compiled .exe has no extra runtime dependencies. */
#include <windows.h>
#include <commctrl.h>
#include <stdio.h>
#include <string.h>

#include "config.h"
#include "engine.h"

#pragma comment(lib, "comctl32.lib")

#define ID_VILLAGE_ENABLED     101
#define ID_VILLAGE_SLIDER      102
#define ID_RP_ENABLED          103
#define ID_RP_SLIDER           104
#define ID_RP_LOOTING          105
#define ID_BT_ENABLED          106
#define ID_BT_SLIDER           107
#define ID_BASTION_ENABLED     108
#define ID_BASTION_BRIDGE      109
#define ID_BASTION_HOUSING     110
#define ID_BASTION_STABLES     111
#define ID_BASTION_TREASURE    112
#define ID_BASTION_SLIDER      113
#define ID_FORTRESS_ENABLED    114
#define ID_FORTRESS_SLIDER     115
#define ID_CHEATS_ENABLED      116
#define ID_SEARCH_BUTTON       117
#define ID_SAVE_BUTTON         118
#define ID_STATUS_LABEL        119
#define ID_VILLAGE_VALUE       120
#define ID_RP_VALUE            121
#define ID_BT_VALUE            122
#define ID_BASTION_VALUE       123
#define ID_FORTRESS_VALUE      124

static FilterConfig g_cfg;
static char g_configPath[MAX_PATH];
static HWND g_hwnd;
static HWND g_controls[125];
static volatile long g_attempts = 0;
static volatile int g_cancel = 0;
static HANDLE g_searchThread = NULL;
static int g_searching = 0;

static HWND mkStatic(HWND parent, const char *text, int x, int y, int w, int h) {
    return CreateWindowA("STATIC", text, WS_CHILD | WS_VISIBLE, x, y, w, h, parent, NULL, NULL, NULL);
}

static HWND mkCheckbox(HWND parent, const char *text, int id, int x, int y, int w, int h, int checked) {
    HWND h2 = CreateWindowA("BUTTON", text, WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
                             x, y, w, h, parent, (HMENU) (INT_PTR) id, NULL, NULL);
    SendMessage(h2, BM_SETCHECK, checked ? BST_CHECKED : BST_UNCHECKED, 0);
    g_controls[id] = h2;
    return h2;
}

static HWND mkSlider(HWND parent, int id, int x, int y, int w, int h, int min, int max, int value) {
    HWND h2 = CreateWindowA(TRACKBAR_CLASSA, "", WS_CHILD | WS_VISIBLE | TBS_AUTOTICKS,
                             x, y, w, h, parent, (HMENU) (INT_PTR) id, NULL, NULL);
    SendMessage(h2, TBM_SETRANGE, TRUE, MAKELONG(min, max));
    SendMessage(h2, TBM_SETPOS, TRUE, value);
    g_controls[id] = h2;
    return h2;
}

static int isChecked(int id) {
    return SendMessage(g_controls[id], BM_GETCHECK, 0, 0) == BST_CHECKED;
}

static int sliderValue(int id) {
    return (int) SendMessage(g_controls[id], TBM_GETPOS, 0, 0);
}

static void updateValueLabel(int labelId, int value) {
    char buf[16];
    snprintf(buf, sizeof(buf), "%d", value);
    SetWindowTextA(g_controls[labelId], buf);
}

static void syncAllValueLabels(void) {
    updateValueLabel(ID_VILLAGE_VALUE, sliderValue(ID_VILLAGE_SLIDER));
    updateValueLabel(ID_RP_VALUE, sliderValue(ID_RP_SLIDER));
    updateValueLabel(ID_BT_VALUE, sliderValue(ID_BT_SLIDER));
    updateValueLabel(ID_BASTION_VALUE, sliderValue(ID_BASTION_SLIDER));
    updateValueLabel(ID_FORTRESS_VALUE, sliderValue(ID_FORTRESS_SLIDER));
}

static void collectConfigFromUi(FilterConfig *cfg) {
    cfg->villageEnabled = isChecked(ID_VILLAGE_ENABLED);
    cfg->villageMaxChunks = sliderValue(ID_VILLAGE_SLIDER);
    cfg->ruinedPortalEnabled = isChecked(ID_RP_ENABLED);
    cfg->ruinedPortalMaxChunks = sliderValue(ID_RP_SLIDER);
    cfg->ruinedPortalRequireLootingSword = isChecked(ID_RP_LOOTING);
    cfg->buriedTreasureEnabled = isChecked(ID_BT_ENABLED);
    cfg->buriedTreasureMaxChunks = sliderValue(ID_BT_SLIDER);
    cfg->bastionEnabled = isChecked(ID_BASTION_ENABLED);
    cfg->bastionAllowBridge = isChecked(ID_BASTION_BRIDGE);
    cfg->bastionAllowHousing = isChecked(ID_BASTION_HOUSING);
    cfg->bastionAllowStables = isChecked(ID_BASTION_STABLES);
    cfg->bastionAllowTreasure = isChecked(ID_BASTION_TREASURE);
    cfg->bastionMaxNetherChunks = sliderValue(ID_BASTION_SLIDER);
    cfg->fortressEnabled = isChecked(ID_FORTRESS_ENABLED);
    cfg->fortressMaxNetherChunks = sliderValue(ID_FORTRESS_SLIDER);
    cfg->enableCheats = isChecked(ID_CHEATS_ENABLED);
    cfg->threadCount = 6;
}

static DWORD WINAPI searchThreadProc(LPVOID param) {
    (void) param;
    FilterResult result;
    g_attempts = 0;
    g_cancel = 0;
    if (engine_search(&g_cfg, &result, &g_attempts, &g_cancel)) {
        char buf[256];
        snprintf(buf, sizeof(buf), "Gefunden! Seed: %lld (Versuche: %ld)", (long long) result.seed, g_attempts);
        SetWindowTextA(g_controls[ID_STATUS_LABEL], buf);

        /* Also write the result so the Minecraft mod (or the user) can pick it up. */
        FILE *f = fopen("seedfilter_result.txt", "w");
        if (f) {
            fprintf(f, "Seed: %lld\nSpawnX: %d\nSpawnZ: %d\nCheats: %d\n",
                    (long long) result.seed, result.spawnX, result.spawnZ, result.enableCheats);
            fclose(f);
        }
    } else {
        SetWindowTextA(g_controls[ID_STATUS_LABEL], "Abgebrochen.");
    }
    g_searching = 0;
    EnableWindow(g_controls[ID_SEARCH_BUTTON], TRUE);
    SetWindowTextA(g_controls[ID_SEARCH_BUTTON], "Suche starten");
    return 0;
}

static LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_HSCROLL:
            syncAllValueLabels();
            return 0;
        case WM_COMMAND:
            if (LOWORD(wParam) == ID_SAVE_BUTTON) {
                collectConfigFromUi(&g_cfg);
                config_save(&g_cfg, g_configPath);
                SetWindowTextA(g_controls[ID_STATUS_LABEL], "Gespeichert.");
            } else if (LOWORD(wParam) == ID_SEARCH_BUTTON) {
                if (g_searching) {
                    g_cancel = 1;
                } else {
                    collectConfigFromUi(&g_cfg);
                    config_save(&g_cfg, g_configPath);
                    g_searching = 1;
                    SetWindowTextA(g_controls[ID_SEARCH_BUTTON], "Abbrechen");
                    SetWindowTextA(g_controls[ID_STATUS_LABEL], "Suche laeuft...");
                    g_searchThread = CreateThread(NULL, 0, searchThreadProc, NULL, 0, NULL);
                }
            }
            return 0;
        case WM_DESTROY:
            g_cancel = 1;
            PostQuitMessage(0);
            return 0;
    }
    return DefWindowProc(hwnd, msg, wParam, lParam);
}

/* mingw's CRT populates these even for a WinMain entry point, so the same .exe can be run
 * headlessly by the Minecraft mod ("seedfilter.exe --search") without opening any window -
 * ProcessBuilder's redirected stdout still works fine on a GUI-subsystem binary. */
extern int __argc;
extern char **__argv;

static int runHeadlessSearch(const char *configPath) {
    FilterConfig cfg;
    config_load(&cfg, configPath);

    volatile long attempts = 0;
    volatile int cancel = 0;
    FilterResult result;

    if (engine_search(&cfg, &result, &attempts, &cancel)) {
        printf("Seed: %lld\n", (long long) result.seed);
        printf("SpawnX: %d\n", result.spawnX);
        printf("SpawnZ: %d\n", result.spawnZ);
        printf("Cheats: %d\n", result.enableCheats);
        printf("Attempts: %ld\n", attempts);
        fflush(stdout);
        return 0;
    }
    printf("NoMatch\n");
    fflush(stdout);
    return 2;
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrev, LPSTR cmdLine, int nCmdShow) {
    (void) hPrev;
    (void) cmdLine;

    strcpy(g_configPath, "seedfilter.cfg");

    for (int i = 1; i < __argc; i++) {
        if (strcmp(__argv[i], "--search") == 0) {
            const char *path = (i + 1 < __argc) ? __argv[i + 1] : g_configPath;
            return runHeadlessSearch(path);
        }
    }

    config_load(&g_cfg, g_configPath);

    INITCOMMONCONTROLSEX icc = {sizeof(icc), ICC_BAR_CLASSES | ICC_STANDARD_CLASSES};
    InitCommonControlsEx(&icc);

    WNDCLASSA wc = {0};
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInstance;
    wc.lpszClassName = "SeedFilterEngineWindow";
    wc.hbrBackground = (HBRUSH) (COLOR_WINDOW + 1);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    RegisterClassA(&wc);

    g_hwnd = CreateWindowA("SeedFilterEngineWindow", "Seed Filter", WS_OVERLAPPEDWINDOW & ~WS_MAXIMIZEBOX,
                            CW_USEDEFAULT, CW_USEDEFAULT, 420, 560, NULL, NULL, hInstance, NULL);

    int y = 12;
    const int rowH = 22, sliderW = 220, labelW = 30;

    mkStatic(g_hwnd, "Ruined Portal", 12, y, 200, 18); y += rowH;
    mkCheckbox(g_hwnd, "Aktiv", ID_RP_ENABLED, 12, y, 100, rowH, g_cfg.ruinedPortalEnabled); y += rowH;
    mkStatic(g_hwnd, "Max. Chunks:", 12, y + 4, 90, 18);
    mkSlider(g_hwnd, ID_RP_SLIDER, 100, y, sliderW, rowH, 1, 32, g_cfg.ruinedPortalMaxChunks);
    g_controls[ID_RP_VALUE] = mkStatic(g_hwnd, "", 100 + sliderW + 8, y + 4, labelW, 18); y += rowH;
    mkCheckbox(g_hwnd, "Looting-2/3-Schwert (langsam)", ID_RP_LOOTING, 12, y, 260, rowH, g_cfg.ruinedPortalRequireLootingSword);
    y += rowH + 10;

    mkStatic(g_hwnd, "Village", 12, y, 200, 18); y += rowH;
    mkCheckbox(g_hwnd, "Aktiv", ID_VILLAGE_ENABLED, 12, y, 100, rowH, g_cfg.villageEnabled); y += rowH;
    mkStatic(g_hwnd, "Max. Chunks:", 12, y + 4, 90, 18);
    mkSlider(g_hwnd, ID_VILLAGE_SLIDER, 100, y, sliderW, rowH, 1, 32, g_cfg.villageMaxChunks);
    g_controls[ID_VILLAGE_VALUE] = mkStatic(g_hwnd, "", 100 + sliderW + 8, y + 4, labelW, 18);
    y += rowH + 10;

    mkStatic(g_hwnd, "Buried Treasure", 12, y, 200, 18); y += rowH;
    mkCheckbox(g_hwnd, "Aktiv", ID_BT_ENABLED, 12, y, 100, rowH, g_cfg.buriedTreasureEnabled); y += rowH;
    mkStatic(g_hwnd, "Max. Chunks:", 12, y + 4, 90, 18);
    mkSlider(g_hwnd, ID_BT_SLIDER, 100, y, sliderW, rowH, 1, 32, g_cfg.buriedTreasureMaxChunks);
    g_controls[ID_BT_VALUE] = mkStatic(g_hwnd, "", 100 + sliderW + 8, y + 4, labelW, 18);
    y += rowH + 10;

    mkStatic(g_hwnd, "Bastion", 12, y, 200, 18); y += rowH;
    mkCheckbox(g_hwnd, "Aktiv", ID_BASTION_ENABLED, 12, y, 100, rowH, g_cfg.bastionEnabled); y += rowH;
    mkCheckbox(g_hwnd, "Bridge", ID_BASTION_BRIDGE, 12, y, 90, rowH, g_cfg.bastionAllowBridge);
    mkCheckbox(g_hwnd, "Housing", ID_BASTION_HOUSING, 110, y, 90, rowH, g_cfg.bastionAllowHousing); y += rowH;
    mkCheckbox(g_hwnd, "Stables", ID_BASTION_STABLES, 12, y, 90, rowH, g_cfg.bastionAllowStables);
    mkCheckbox(g_hwnd, "Treasure", ID_BASTION_TREASURE, 110, y, 90, rowH, g_cfg.bastionAllowTreasure); y += rowH;
    mkStatic(g_hwnd, "Max. Nether-Chunks:", 12, y + 4, 90, 18);
    mkSlider(g_hwnd, ID_BASTION_SLIDER, 100, y, sliderW, rowH, 1, 32, g_cfg.bastionMaxNetherChunks);
    g_controls[ID_BASTION_VALUE] = mkStatic(g_hwnd, "", 100 + sliderW + 8, y + 4, labelW, 18);
    y += rowH + 10;

    mkStatic(g_hwnd, "Fortress", 12, y, 200, 18); y += rowH;
    mkCheckbox(g_hwnd, "Aktiv", ID_FORTRESS_ENABLED, 12, y, 100, rowH, g_cfg.fortressEnabled); y += rowH;
    mkStatic(g_hwnd, "Max. Nether-Chunks:", 12, y + 4, 90, 18);
    mkSlider(g_hwnd, ID_FORTRESS_SLIDER, 100, y, sliderW, rowH, 1, 32, g_cfg.fortressMaxNetherChunks);
    g_controls[ID_FORTRESS_VALUE] = mkStatic(g_hwnd, "", 100 + sliderW + 8, y + 4, labelW, 18);
    y += rowH + 10;

    mkCheckbox(g_hwnd, "Cheats aktivieren", ID_CHEATS_ENABLED, 12, y, 200, rowH, g_cfg.enableCheats);
    y += rowH + 10;

    CreateWindowA("BUTTON", "Speichern", WS_CHILD | WS_VISIBLE, 12, y, 100, 28, g_hwnd, (HMENU) (INT_PTR) ID_SAVE_BUTTON, NULL, NULL);
    g_controls[ID_SEARCH_BUTTON] = CreateWindowA("BUTTON", "Suche starten", WS_CHILD | WS_VISIBLE, 120, y, 140, 28, g_hwnd, (HMENU) (INT_PTR) ID_SEARCH_BUTTON, NULL, NULL);
    y += 36;
    g_controls[ID_STATUS_LABEL] = mkStatic(g_hwnd, "Bereit.", 12, y, 380, 40);

    syncAllValueLabels();

    ShowWindow(g_hwnd, nCmdShow);
    UpdateWindow(g_hwnd);

    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }
    return 0;
}
