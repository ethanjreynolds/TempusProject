async function get_zone_runs(event, element, map_id, zone_index) {
    const tfclass = element.dataset.class;
    const zone_type = element.dataset.zoneType;

    let all = false;
    if (event.shiftKey) all = true;

    let table = document.getElementById(`${tfclass}-runs`);
    let res = await fetch(`/api/maps/${map_id}/${zone_type}/${zone_index}/runs?tfclass=${tfclass}&offset=${table.rows.length}${all ? '' : '&limit=50'}`);

    if (res.ok) (await res.json()).forEach(row => table.insertAdjacentHTML("beforeend", `<tr><td class="num _${row.group}">${row.rank}</td><td class="num">${formatDuration(row.duration)}</td><td><a href="/players/${row.player.id}">${row.player.name}</a></td><td class="num">${formatDate(row.time_submitted)}</td></tr>`));
}