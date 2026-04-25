export type MeetingMessage = {
    id: number;
    sender: string;
    time: string;
    text: string;
    isYou: boolean;
};

export const INITIAL_MEETING_MESSAGES: MeetingMessage[] = [
    {
        id: 1,
        sender: 'Sarah Jenkins',
        time: '10:42 AM',
        text: "Welcome everyone! Let's start with the Q3 review.",
        isYou: false,
    },
    {
        id: 2,
        sender: 'Elena Rodriguez',
        time: '10:43 AM',
        text: "I've updated the Figma file with the latest prototypes.",
        isYou: false,
    },
    {
        id: 3,
        sender: 'You',
        time: '10:45 AM',
        text: 'The new video grid logic looks great. Checking performance now.',
        isYou: true,
    },
    {
        id: 4,
        sender: 'Marcus Thorne',
        time: '10:48 AM',
        text: 'Agreed. @Elena, can you share the link?',
        isYou: false,
    },
];
