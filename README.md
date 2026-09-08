# TEMPUS Project

## User Manual

**Hansen Chai, Ethan Reynolds**

---

## Project Deployment Instructions for Arch Linux

### 1. Update All System Packages

```bash
sudo pacman -Syu
```

### 2. Install All Project Dependencies

```bash
sudo pacman -S jdk21-openjdk postgresql
```

### 3. Set Up PostgreSQL

1. Change to the `postgres` user:

   ```bash
   sudo -u -i postgres
   ```

2. Initialize the PostgreSQL database:

   ```bash
   [postgres]$ initdb -D /var/lib/postgres/data
   ```

3. Enable and start PostgreSQL:

   ```bash
   $ sudo systemctl enable --now postgresql
   ```

4. Create a PostgreSQL user:

   ```bash
   [postgres]$ createuser --interactive
   ```

### 4. Clone the Repository

```bash
git clone git@github.com:PSU-CMPSC431W/cmpsc431w-fa25-project-ctrl-alt-elite.git
```

### 5. Navigate to the Web Application Directory

```bash
cd cmpsc431w-fa25-project-ctrl-alt-elite/tempus/
```

### 6. Load the SQL Files

Run the following commands:

```bash
[postgres]$ psql < db.sql
```

```bash
[postgres]$ psql < zone_run_scored_view.sql
```

```bash
[postgres]$ psql < player_stats_view.sql
```

### 7. Configure the Application

Create the following file:

```text
tempus/src/main/resources/application.properties
```

Fill it out with the following properties:

```properties
spring.application.name=
spring.datasource.url=
spring.datasource.username=
```

### 8. Build the Project

```bash
./gradlew build
```

### 9. Run the JAR File

```bash
java -jar build/libs/tempus-0.1.jar
```

### 10. Open the Web Application

Navigate to:

```text
http://localhost:8080
```

---

# Using the Project

## Maps Tab

### List of All Maps

- The Maps tab displays a grid containing every available map.
- A sidebar on the left allows you to sort the maps by several options, including:
  - ID
  - Name
  - Tier
  - Rating
- Clicking the currently selected sorting type changes the sorting direction between ascending and descending.

## Specific Map Tab

### Map/Zone Leaderboard

- Clicking on any map takes you to per-class leaderboards for that map, sorted by rank.
- Scrolling down reveals a **Load More** button, which allows you to expand the number of players displayed on the leaderboard.
- You can also **Shift + Click** the Load More button to load all runs.
- The sidebar may contain different **zones**. A map may have multiple courses, which represent parts of the entire map, as well as bonuses.

---

## Players Tab

### Adding Players

- At the top of the page, there is an option to add a new player.
- Enter the following information:
  - **Name**
  - **Steam ID** *(optional)*
  - **Country*
- Click **Submit**.
- The new player will be created in the database, and the application will provide a link to the new player's page.

### Player Leaderboard

- The Players tab contains a leaderboard of all players, ordered by rank in ascending order.
- Clicking **Overall**, **Demoman**, or **Soldier** displays the leaderboard using only the points from the selected class.

---

## Specific Player Tab

### Adding, Updating, and Deleting Runs

At the top of the page, there is a boxed option to **Add a New Zone Run**.

To add a run:

1. Edit the values in the available text boxes and dropdown menus.
2. Fill in all required options.
3. Click **Submit** to add the run for that player.

To delete a run:

1. Click the **trash can icon** on the right side of the run.
2. Click **OK** to confirm the deletion.

### Player Statistics

- Clicking on a player from either the player leaderboard or a map leaderboard takes you to that player's statistics page.
- The statistics page displays information such as:
  - Runs
  - Overall rank
  - Other player statistics
- The page also contains a leaderboard of the player's best runs.
- The best-runs leaderboard can be sorted by criteria such as:
  - Rank
  - Duration
  - Other available sorting options
