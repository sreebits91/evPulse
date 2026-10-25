import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "evPulse | Find your charge",
  description: "Compare EV charging options and tariff transparency in Hyderabad.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}