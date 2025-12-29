function formatDate(date) {
    return new Date(date).toISOString().split('T')[0];
}

function formatDuration(duration) {
    const HH = Math.floor(duration / 3600);
    const MM = Math.floor((duration % 3600) / 60);
    const SS = Math.floor(duration % 60);
    const mmm = Math.floor((duration * 1000) % 1000);

    if (duration > 3600) return `${HH}:${String(MM).padStart(2, '0')}:${String(SS).padStart(2, '0')}.${String(mmm).padStart(3, '0')}`;
    if (duration > 60) return `${String(MM).padStart(2, '0')}:${String(SS).padStart(2, '0')}.${String(mmm).padStart(3, '0')}`;
    if (duration > 0) return `${String(SS).padStart(2, '0')}.${String(mmm).padStart(3, '0')}`;
    return `0.${String(mmm).padStart(3, '0')}`;
}
