export const MAP_MOVE = 'MAP_MOVE';

export function mapMove(zoom, center) {
    return {
        type: MAP_MOVE,
        map: {
            zoom: zoom,
            center: center
        }

    };
}