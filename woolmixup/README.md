# WoolMixUp — solo minigra w stylu "Wool Mix Up" (Minecraft Party)

Plansza z kolorowej welny. Co rundę losujemy kolor i pokazujemy go na ekranie
(tytuł + dźwięk). Musisz zdążyć stanąć na bloku tego koloru, zanim czas runy
się skończy — wtedy cała reszta plansz zamienia się w powietrze i jeśli nie
stoisz na dobrym kolorze, spadasz i przegrywasz. Z rundy na rundę robi się
szybciej. Wynik = liczba przetrwanych rund, zapisywana lokalnie jako Twój
rekord (`/woolmixup best`).

Działa samodzielnie — nie wymaga `DynamoSync` ani żadnego innego pluginu.

## 1. Zbuduj plugin

Ten sandbox nie ma dostępu do repozytoriów Mavena, więc jar trzeba zbudować
przez dołączony workflow GitHub Actions (`.github/workflows/build.yml`) albo
lokalnie.

**Ważne przy wgrywaniu na GitHub przez "Add files via upload":** wgrywaj
zawartość tego folderu (pliki `build.gradle.kts`, `settings.gradle.kts`,
`src/`, `.github/` bezpośrednio), a NIE folder-wrapper wokół nich. Jeśli
przeciągniesz cały folder `woolmixup`, GitHub utworzy dodatkowy poziom
zagnieżdżenia (`woolmixup/woolmixup/...`) i workflow (który zakłada, że
`build.gradle.kts` leży w korzeniu repo) się wywali z tym samym błędem co
poprzednio przy DynamoSync. Workflow ma teraz krok "Debug - pokaz strukture
repo", który od razu pokaże Ci prawdziwy układ plików, jeśli coś pójdzie nie
tak.

Budowanie lokalnie (wymaga JDK 25):

```bash
gradle jar
```

Gotowy jar: `build/libs/WoolMixUp-1.0.0.jar` → wrzuć do `plugins/` na
serwerze.

## 2. Stwórz arenę

W grze, jako OP:

```
/woolmixup setup <nazwa> [rozmiar]
```

Np. `/woolmixup setup arena1 9` tworzy planszę 9x9 na Twojej aktualnej
pozycji (Twoja pozycja = południowo-zachodni róg planszy, podłoga na
bloku pod Twoimi stopami). Rozmiar 5–21, domyślnie 9.

Domyślnie każdy kolor zajmuje kwadrat **3x3 bloki** (konfigurowalne przez
`game.cell-size` w configu) — czyli przy planszy 9x9 masz 3x3 = 9 takich
kafli. Jeśli chcesz więcej różnorodności kolorów na planszy, użyj większego
rozmiaru, np. `/woolmixup setup arena1 15` (5x5 = 25 kafli przy cell-size 3).

Ustaw arenę na płaskim, pustym terenie z odrobiną przestrzeni pod spodem
(2-3 bloki), żeby "spadnięcie" wyglądało naturalnie i nie zabijało gracza
fall-damage'em — spadnięcie kończy grę zanim gracz zdąży spaść wysoko.

## 3. Zagraj

```
/woolmixup start [nazwa]        # jeśli masz tylko 1 arenę, nazwa jest opcjonalna
/woolmixup stop                 # przerywa aktualną gre
/woolmixup best                 # pokazuje Twój rekord (liczba przetrwanych rund)
/woolmixup list                 # lista wszystkich aren
```

## 4. Konfiguracja (`plugins/WoolMixUp/config.yml`)

```yaml
game:
  base-time-seconds: 5.0          # czas na pierwszą rundę
  min-time-seconds: 1.3           # dolny limit czasu
  time-decrease-per-round: 0.15   # o ile sekund krócej każda kolejna runda
  countdown-before-round-seconds: 1.2
  fall-threshold-blocks: 2        # ile bloków spadku = koniec gry
  colors: [WHITE, ORANGE, MAGENTA, LIGHT_BLUE, YELLOW, LIME, PINK, CYAN, PURPLE, RED]
settings:
  debug: false
```

## 5. Integracja z DynamoSync (opcjonalnie)

Wyniki trzymane są domyślnie lokalnie w `plugins/WoolMixUp/scores.yml`. Jeśli
wolisz trzymać je w tej samej tabeli DynamoDB co reszta danych gracza (masz
już do tego infrastrukturę w DynamoSync), podepnij się pod publiczne API:

```java
PlayerDataManager mgr = DynamoSyncPlugin.getInstance().getDataManager();
PlayerData data = mgr.getCached(player.getUniqueId());
if (data != null) {
    data.setCustom("woolmixup_best_round", String.valueOf(finishedRounds));
    mgr.save(data);
}
```

Najprościej wstawić to wywołanie w `GameSession.fail()` zamiast/obok
`scoreStorage.submitScore(...)`. Zrobię to za Ciebie, jeśli wolisz mieć jedno
źródło prawdy zamiast dwóch osobnych plików wyników.
