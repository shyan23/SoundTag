import type { Metadata } from "next";
import "./globals.css";
import { SoundTagProvider } from "./providers/soundtag-context";
import { Snackbar } from "./components/snackbar";

export const metadata: Metadata = {
  title: "SoundTag",
  description: "Urban Noise Dataset Collector",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <SoundTagProvider>
          {children}
          <Snackbar />
        </SoundTagProvider>
      </body>
    </html>
  );
}
