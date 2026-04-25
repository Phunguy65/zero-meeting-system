export type GreenRoomAttendee = {
    name: string;
    initials: string;
    palette: string;
};

export const GREEN_ROOM_ATTENDEES: GreenRoomAttendee[] = [
    {
        name: 'Sarah Chen',
        initials: 'SC',
        palette:
            'from-[var(--avatar-gradient-dark-start)] to-[var(--avatar-gradient-dark-end)]',
    },
    {
        name: 'Marcus Wright',
        initials: 'MW',
        palette:
            'from-[var(--avatar-gradient-blue-start)] to-[var(--avatar-gradient-blue-end)]',
    },
    {
        name: 'Elena Rodriguez',
        initials: 'ER',
        palette:
            'from-[var(--avatar-gradient-red-start)] to-[var(--avatar-gradient-red-end)]',
    },
];
