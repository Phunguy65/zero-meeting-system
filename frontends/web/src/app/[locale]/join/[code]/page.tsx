import { JoinMeetingContainer } from '@/components/join-meeting/index.tsx';

type GuestJoinPageProps = {
    params: Promise<{ code: string }>;
};

export default async function GuestJoinPage({ params }: GuestJoinPageProps) {
    const { code } = await params;
    return <JoinMeetingContainer initialCode={code} mode='guest' />;
}
